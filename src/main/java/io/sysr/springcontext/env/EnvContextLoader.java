package io.sysr.springcontext.env;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
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

/**
 * The {@code EnvContextLoader} class is responsible for loading environment
 * variables from <code>.env</code> files or an <code>env.properties</code> file
 * into a properties map.
 * It handles variable resolution, including nested variables, validates
 * variable names and detects circular dependencies.
 *
 * <p>
 * <b> Example usage: </b>
 * </p>
 * 
 * <pre>{@code
 * // Create an instance of the loader
 * EnvContextLoader loader = new EnvContextLoader();
 *
 * // Load environment variables
 * loader.load();
 *
 * // Retrieve the loaded properties
 * Properties properties = loader.getLoadedProperties();
 *
 * // Access a property
 * String dbUrl = properties.getProperty("DATABASE_URL");
 * }</pre>
 *
 * <p>
 * <b>Note:</b> Ensure that the dotenv properties configuration file
 * (<b>env.properties</b>) is placed in the resources foler. This only applies
 * if you have the <b>.env</b> file that contains the environment declarions is
 * placed in a custom location.
 * </p>
 *
 * @author Calvince Otieno
 * @version 1.0.0
 * @since 2024
 */
public class EnvContextLoader {
    private static final Logger logger = LoggerFactory.getLogger(EnvContextLoader.class);
    /**
     * A concurrent map storing all loaded and resolved environment properties.
     */
    private final ConcurrentHashMap<String, String> propertiesMap = new ConcurrentHashMap<>();

    private static final Pattern ENV_FILE_NAME_PATTERN = Pattern.compile("^\\.env\\.?\\w*$");
    private static final Pattern VARIABLE_PATTERN_MATCHER = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_-]*$");
    private static final Pattern BAD_VARIABLE_PATTERN_MATCHER = Pattern.compile("\\$\\{\\s*\\}$|\\$\\{[^}]*$");

    /**
     * Constructs a new {@code EnvContextLoader} instance.
     */
    public EnvContextLoader() {
        super();
        logger.trace("Spring context dotenv loader initiated");
    }

    /**
     * Retrieves the loaded properties as a {@link java.util.Properties} object.
     *
     * @return The {@link Properties} object containing all loaded and resolved
     *         environment variables.
     */
    public Properties getLoadedProperties() {
        Properties props = new Properties();
        propertiesMap.entrySet().forEach(entry -> props.put(entry.getKey(), entry.getValue()));
        return props;
    }

    /**
     * Initiates the loading process of environment variables.
     *
     * <p>
     * It attempts to load variables from a user-provided file which is specified in
     * the <code>env.properties</code> file or from <code>.env</code> files in the
     * default root directory.
     * </p>
     *
     * @throws EnvContextLoaderException if there is any error during the loading
     *                                   process.
     */
    public void load() {
        try {
            // Load any default .env files found in the root of the project
            loadEnvFilesFromDirectory(System.getProperty("user.dir"));

            // Look for user defined env files
            String userProvidedFilePath = findEnvPropertiesFile();
            if (Objects.nonNull(userProvidedFilePath)) {
                loadEnvFilesFromUserDefinedPath(userProvidedFilePath);
            }

            // Look for System.env for DIR_PATH.
            // This is manly applicable when running a containerized app
            String dirPath = System.getenv("ENV_DIR_PATH");
            if (Objects.nonNull(dirPath)) {
                // Replace single backslashes with double backslashes for Windows paths
                dirPath = dirPath.replace("\\", "\\\\");
                loadEnvFilesFromDirectory(dirPath);
            }

        } catch (Exception e) {
            throw new EnvContextLoaderException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Loads environment variables from a user-provided <code>.env</code>
     * files.
     *
     * <p>
     * Reads the <code>BASE_DIR</code> and locates the files specified by the
     * <code>FILE*</code> properties to locate and load <code>.env</code> files.
     * </p>
     *
     * @param envPropertiesFilePath The file path to the <code>env.properties</code>
     *                              file provided by the user.
     * @throws EnvContextLoaderException if there is an error reading the properties
     *                                   or loading the files.
     */
    private void loadEnvFilesFromUserDefinedPath(String envPropertiesFilePath) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(Path.of(
                envPropertiesFilePath)),
                StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(reader);
            String directoryPath = Stream.of("ENV_DIR_PATH")
                    .map(key -> props.getProperty(key.toUpperCase()))
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
                logger.warn("BASE_DIR not found in the properties file - {}", envPropertiesFilePath);
            }
        } catch (Exception e) {
            throw new EnvContextLoaderException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Loads <code>.env</code> files from the directory returned by
     * <code>System.getProperty("user.dir")</code> or specified by Environment
     * Property <code>ENV_DIR_PATH</code>.
     *
     * @param filePath The root directory path where <code>.env</code> files are
     *                 located.
     * @throws EnvContextLoaderException if there is an error reading the directory
     *                                   or loading the files.
     */
    private void loadEnvFilesFromDirectory(String filePath) {
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

    /**
     * Parses a <code>.env</code> file and resolves the environment variables within
     * it.
     *
     * @param path The path to the <code>.env</code> file to be parsed.
     * @throws EnvContextLoaderException if there is an error reading or parsing the
     *                                   file.
     */
    private void parse(Path path) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(reader);
            for (String key : props.stringPropertyNames()) {
                String value = getResolvedValue(props, key);
                if (Objects.nonNull(value) && !value.isBlank()) {
                    propertiesMap.put(key.strip(), value.strip());
                }
            }
        } catch (Exception e) {
            throw new EnvContextLoaderException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Resolves the value of a given environment variable, handling nested variables
     * and detecting circular dependencies.
     *
     * @param props The {@link Properties} object containing the environment
     *              variables.
     * @param key   The name of the variable to resolve.
     * @return The resolved value of the variable, or {@code null} if it cannot be
     *         resolved.
     * @throws EnvContextLoaderException if a circular dependency is detected.
     */
    private String getResolvedValue(Properties props, String key) {
        Map<String, String> resolved = new HashMap<>();
        Set<String> resolving = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();

        // If the variable name in the env properties file is invalid then stop further
        // actions and return immediately.
        if (!isValidVariableName(key)) {
            logger.warn("The variable name: {} is considered invalid. Please double check.", key);
            return null;
        }

        // Push key to the stack
        stack.push(key);

        // Resolve the value of the variable marked by key
        while (!stack.isEmpty()) {
            String name = stack.peek();
            // When a variable value defined use its value
            // otherwise, fall back to the system property with the same name.
            String value = props.getProperty(name);
            if (Objects.isNull(value) || value.isBlank()) {
                value = System.getenv(name);
                if (Objects.isNull(value) || value.isBlank()) {
                    value = System.getProperty(name);
                }
            }

            if (Objects.isNull(value) || value.isBlank()) {
                logger.warn("The definition of the env variable {} is not found!", name);
                // We cannot resolve this variable
                return null;
            }

            // Check if the value is valid
            Matcher badVariable = BAD_VARIABLE_PATTERN_MATCHER.matcher(value);
            if (badVariable.matches()) {
                logger.warn("The variable definition {}={} is considered invalid. Please double check.", name, value);
                // We cannot proceed further, this variable will not be resolved
                return null;
            } else {
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
        }
        return resolved.get(key);
    }

    /**
     * Helper method to iteratively resolve a variable's value.
     *
     * @param resolving       A set of variable names currently being resolved.
     * @param resolved        A map of already resolved variables and their values.
     * @param stack           A stack used for depth-first resolution of variables.
     * @param variableMatcher A {@link Matcher} object to find variable patterns in
     *                        the values.
     * @param sb              A {@link StringBuilder} to construct the resolved
     *                        value.
     * @return {@code true} if the variable was successfully resolved, {@code false}
     *         if further resolution is needed.
     * @throws EnvContextLoaderException if a circular dependency is detected.
     */
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

    /**
     * Searches for the <code>env.properties</code> file within the classpath
     * resources.
     *
     * @return The path to the <code>env.properties</code> file if found, or
     *         {@code null} if not found.
     * @throws URISyntaxException        If the resource URL syntax is incorrect.
     * @throws IOException               If an I/O error occurs accessing the file
     *                                   system.
     * @throws EnvContextLoaderException If the classpath root is not a directory.
     */
    private String findEnvPropertiesFile() throws URISyntaxException {
        URL resourceUrl = getClass().getClassLoader().getResource("dotenv.properties");
        if (Objects.nonNull(resourceUrl)) {
            Path path = Path.of(resourceUrl.toURI());
            return path.toAbsolutePath().toString();

        }
        logger.warn("dotenv.properties not found in the classpath");
        return null;
    }

    /**
     * Validates if a variable name adheres to the allowed pattern, which starts
     * with a letter or underscore, followed by letters, digits, underscores, or
     * hyphens.
     *
     * @param variableName The name of the variable to validate.
     * @return {@code true} if the variable name is valid, {@code false} otherwise.
     */
    private boolean isValidVariableName(String variableName) {
        Matcher matcher = VARIABLE_NAME_PATTERN.matcher(variableName);
        return matcher.matches();
    }
}
