package io.sysr.springcontext.env;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.sysr.springcontext.env.exception.EnvContextLoaderException;

public class EnvContextLoader {
    private static final Logger logger = LoggerFactory.getLogger(EnvContextLoader.class);
    private final ConcurrentHashMap<String, String> propertiesMap = new ConcurrentHashMap<>();
    private static final Pattern ENV_FILE_NAME_PATTERN = Pattern.compile("^\\.env\\.?\\w*$");
    private static final Pattern VARIABLE_PATTERN_MATCHER = Pattern.compile("^.*?\\$\\{([^}]+)}$");
    private static final Pattern BAD_VARIABLE_PATTERN_MATCHER = Pattern
            .compile("^(\\$\\{[^}]*|\\$?\\w+|\\$\\{\\})\\}$");

    public EnvContextLoader() {
        super();
        logger.trace("Spring context dot env loader initiated");

    }

    public Properties getLoadedProperties() {
        Properties props = new Properties();
        propertiesMap.entrySet().forEach(entry -> props.put(entry.getKey(), entry.getValue()));
        return props;
    }

    public void load() {
        try {
            String userProvidedFilePath = getEnvConfigurationFilePath();

            if (Objects.nonNull(userProvidedFilePath)) {
                loadFromUserProvidedDirectory(userProvidedFilePath);
            } else {
                loadFromDefaultRootDirectory(System.getProperty("user.dir"));
            }
        } catch (Exception e) {
            throw new EnvContextLoaderException(e.getLocalizedMessage(), e);
        }
    }

    private void loadFromUserProvidedDirectory(String filePath) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(Path.of(filePath)),
                StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(reader);
            String directoryPath = Stream.of("BASE_DIR", "base_dir")
                    .map(props::getProperty)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (Objects.nonNull(directoryPath)) {
                List<String> fileNameKeys = props.stringPropertyNames()
                        .stream().filter(key -> key.toUpperCase().startsWith("FILE"))
                        .toList();

                for (String fileNameKey : fileNameKeys) {
                    String fileName = props.getProperty(fileNameKey);
                    Path path = Path.of(directoryPath).normalize().resolve(fileName).normalize();
                    File file = path.toFile();
                    if (file.exists() && file.isFile()) {
                        parse(path);
                        logger.info("Successfully loaded properties from {}", path.toAbsolutePath());
                    }
                }
            } else {
                logger.warn("Neither BASE_DIR nor base_dir property found in the properties file - {}", filePath);
            }
        } catch (Exception e) {
            throw new EnvContextLoaderException(e.getLocalizedMessage(), e);
        }
    }

    private void loadFromDefaultRootDirectory(String filePath) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Path.of(filePath))) {
            for (Path path : directoryStream) {
                File file = path.toFile();
                if (file.isFile() && file.getName().matches(ENV_FILE_NAME_PATTERN.pattern())) {
                    parse(path.normalize());
                    logger.info("Successfully loaded properties from {}", path.toAbsolutePath());
                }
            }
        } catch (Exception e) {
            throw new EnvContextLoaderException(e.getLocalizedMessage(), e);
        }
    }

    private void parse(Path path) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(reader);
            for (String key : props.stringPropertyNames()) {
                String value = getResolvedValue(props, key);
                if (Objects.nonNull(value)) {
                    propertiesMap.put(key.strip(), value.strip());
                }
            }
        } catch (Exception e) {
            throw new EnvContextLoaderException(e.getLocalizedMessage(), e);
        }
    }

    private String getResolvedValue(Properties props, String key) {
        Map<String, String> resolved = new HashMap<>();
        Set<String> resolving = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();

        // Push key to the stack
        stack.push(key);

        // Resolve the value of the variable marked by key
        while (!stack.isEmpty()) {
            String name = stack.peek();
            String value = props.getProperty(name);

            if (Objects.isNull(value)) {
                // check environment variable
                value = System.getProperty(name);
                if (Objects.isNull(value)) {
                    logger.warn("The definition of the env variable {} is not found!", name);
                    logger.warn("Variable {} resolution is skipped.", name);
                    // Remove this variable name form the queue
                    stack.pop();
                }
            }

            // Check if the value is valid one
            Matcher badVariable = BAD_VARIABLE_PATTERN_MATCHER.matcher(value);
            if (badVariable.find()) {
                logger.warn("The variable definition {}={} is considered invalid. Please double check.", name, value);
                logger.warn("Variable {} resolution is skipped.", name);
                // Remove this variable name form the queue
                stack.pop();
            }

            // Add variable name to resolving set
            resolving.add(name);

            // Iteratively resolve the variable value
            Matcher variableMatcher = VARIABLE_PATTERN_MATCHER.matcher(value);
            StringBuilder sb = new StringBuilder();
            boolean isResolved = resolve(resolving, resolved, stack, variableMatcher, sb);

            if (isResolved) {
                variableMatcher.appendTail(sb);
                resolved.put(name, sb.toString());
                resolving.remove(name);
                stack.pop();
            }
        }
        return resolved.get(key);
    }

    private boolean resolve(Set<String> resolving, Map<String, String> resolved, Deque<String> stack,
            Matcher variableMatcher, StringBuilder sb) {

        boolean isResolved = true;

        while (variableMatcher.find()) {
            String name = variableMatcher.group(1);

            if (resolving.contains(name)) {
                throw new EnvContextLoaderException("Circular dependency detected on variable %s.".formatted(name));
            }

            if (resolved.containsKey(name)) {
                // use the resolved value from the local resolved cache
                variableMatcher.appendReplacement(sb, resolved.get(name));
            } else if (propertiesMap.containsKey(name)) {
                // use the resolved value from the already resolved properties map
                variableMatcher.appendReplacement(sb, propertiesMap.get(name.strip()));
            } else {
                stack.push(name);
                isResolved = false;
            }
        }
        return isResolved;
    }

    private String getEnvConfigurationFilePath() {
        URL resourceUrl = getClass().getResource("/");
        if (Objects.nonNull(resourceUrl)) {
            Path path = Path.of(resourceUrl.getPath()).resolve("resources/env.properties");
            if (Objects.nonNull(path) && Files.exists(path)) {
                return path.toString();
            }
        }
        logger.warn("env.properties not found in any 'resources' directory in the classpath");
        return null;
    }
}
