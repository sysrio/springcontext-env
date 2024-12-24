package io.sysr.springcontext.env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvContextLoaderTest {
    private static final String ENV_PROPERTIES_CONFIG_FILE_NAME = "env.properties";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Nothing at the moment
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir).map(Path::toFile).forEach(File::delete);
    }

    @Test
    void whenEnvPropertiesFileExists_thenMethodReturnsPath() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, SecurityException, IOException, URISyntaxException {
        // Get the root URL from the class loader
        URL url = EnvContextLoader.class.getClassLoader().getResource("");
        Path root = Path.of(url.toURI());

        // Create the resources directory and env.properties file
        Path resourcesDirPath = root.resolve("resources");
        Files.createDirectories(resourcesDirPath);
        Path envFile = resourcesDirPath.resolve(ENV_PROPERTIES_CONFIG_FILE_NAME);
        Files.createFile(envFile);

        try {
            EnvContextLoader envContextLoader = new EnvContextLoader();
            Method method = EnvContextLoader.class.getDeclaredMethod("findEnvPropertiesFile");
            method.setAccessible(true);
            String path = (String) method.invoke(envContextLoader);

            assertThat(path).isNotNull().endsWith(ENV_PROPERTIES_CONFIG_FILE_NAME);
            assertThat(new File(path)).exists().isFile();
            assertThat(path).isEqualTo(envFile.toString());
        } finally {
            Files.deleteIfExists(envFile);
            Files.deleteIfExists(resourcesDirPath);
        }
    }

    @Test
    void whenEnvPropertiesFileNotFound_thenMethodReturnsNull() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, SecurityException, URISyntaxException {
        // Get the root URL from the class loader
        URL url = EnvContextLoader.class.getClassLoader().getResource("");
        Path root = Path.of(url.toURI());

        // Ensure the env.properties file does not exist
        Path resourcesDirPath = root.resolve("resources");
        Path envFile = resourcesDirPath.resolve(ENV_PROPERTIES_CONFIG_FILE_NAME);

        if (Files.exists(envFile)) {
            fail("Test failed because env.properties file exists when it should not.");
        }

        EnvContextLoader envContextLoader = new EnvContextLoader();
        Method method = EnvContextLoader.class.getDeclaredMethod("findEnvPropertiesFile");
        method.setAccessible(true);
        String path = (String) method.invoke(envContextLoader);

        assertThat(path).isNull();
    }

    @Test
    void whenUserProvidedValidPropertiesFilePath_thenTheFileContentMustBeLoadedSuccessfully() throws URISyntaxException,
            IOException, NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        Path userDir = tempDir.resolve("config");
        Files.createDirectories(userDir);
        Path filePath = userDir.resolve("env");
        Files.createFile(filePath);
        Files.writeString(filePath, "KEY=VALUE", StandardCharsets.UTF_8);

        // Get the root URL from the class loader
        URL url = EnvContextLoader.class.getClassLoader().getResource("");
        Path root = Path.of(url.toURI());

        // Create the resources directory and env.properties file
        Path resourcesDirPath = root.resolve("resources");
        Files.createDirectories(resourcesDirPath);
        Path envFile = resourcesDirPath.resolve(ENV_PROPERTIES_CONFIG_FILE_NAME);
        Files.createFile(envFile);
        Files.writeString(envFile,
                "BASE_DIR=%s%nFILE_NAME=%s%n".formatted(
                        userDir.toString().replace("\\", "\\\\"),
                        filePath.getFileName().toString()));

        try {
            EnvContextLoader envContextLoader = new EnvContextLoader();
            Method findEnvPropertiesFileMethod = EnvContextLoader.class.getDeclaredMethod("findEnvPropertiesFile");

            findEnvPropertiesFileMethod.setAccessible(true);
            String path = (String) findEnvPropertiesFileMethod.invoke(envContextLoader);

            assertThat(path).isNotNull().endsWith(ENV_PROPERTIES_CONFIG_FILE_NAME);
            assertThat(new File(path)).exists().isFile();
            assertThat(path).isEqualTo(envFile.toString());

            // Load file from user dir
            Method loadFromUserDirMethod = EnvContextLoader.class.getDeclaredMethod(
                    "loadFromUserProvidedDirectory",
                    String.class);

            loadFromUserDirMethod.setAccessible(true);
            loadFromUserDirMethod.invoke(envContextLoader, path);

            Properties props = envContextLoader.getLoadedProperties();
            assertThat(props)
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(1);

            assertThat(props.stringPropertyNames())
                    .contains("KEY");

            assertThat(props.getProperty("KEY"))
                    .isEqualTo("VALUE");

        } finally {
            Files.deleteIfExists(envFile);
            Files.deleteIfExists(resourcesDirPath);
        }
    }

    @Test
    void whenUserProvidedInvalidPropertiesFilePath_thenNothingIsLoaded() throws URISyntaxException, IOException,
            NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {

        // Get the root URL from the class loader
        URL url = EnvContextLoader.class.getClassLoader().getResource("");
        Path root = Path.of(url.toURI());

        // Create the resources directory and env.properties file
        Path resourcesDirPath = root.resolve("resources");
        Files.createDirectories(resourcesDirPath);
        Path envFile = resourcesDirPath.resolve(ENV_PROPERTIES_CONFIG_FILE_NAME);
        Files.createFile(envFile);
        Files.writeString(envFile,
                "BASE_DIR=%s%nFILE_NAME=%s%n".formatted(
                        tempDir.toString().replace("\\", "\\\\"),
                        "nofile"));

        try {
            EnvContextLoader envContextLoader = new EnvContextLoader();
            Method findEnvPropertiesFileMethod = EnvContextLoader.class.getDeclaredMethod("findEnvPropertiesFile");

            findEnvPropertiesFileMethod.setAccessible(true);
            String path = (String) findEnvPropertiesFileMethod.invoke(envContextLoader);

            assertThat(path).isNotNull().endsWith(ENV_PROPERTIES_CONFIG_FILE_NAME);
            assertThat(new File(path)).exists().isFile();
            assertThat(path).isEqualTo(envFile.toString());

            // Load file from user dir
            Method loadFromUserDirMethod = EnvContextLoader.class.getDeclaredMethod(
                    "loadFromUserProvidedDirectory",
                    String.class);

            loadFromUserDirMethod.setAccessible(true);
            loadFromUserDirMethod.invoke(envContextLoader, path);

            Properties props = envContextLoader.getLoadedProperties();
            assertThat(props)
                    .isNotNull()
                    .isEmpty();

        } finally {
            Files.deleteIfExists(envFile);
            Files.deleteIfExists(resourcesDirPath);
        }
    }

    @Test
    void whenDefaultRootLoaderIsTriggeredAndEnvFileIsAvailable_thenTheFileContentMustBeLoadedSuccessfully() throws
                URISyntaxException, IOException, NoSuchMethodException, SecurityException, IllegalAccessException,
                InvocationTargetException {
        Path rootPath = Files.createDirectories(tempDir.resolve("springcontext-env"));
        Path envFile = Files.createFile(rootPath.resolve(".env"));
        Files.writeString(envFile,"KEY=VALUE", StandardCharsets.UTF_8);

        EnvContextLoader envContextLoader = new EnvContextLoader();
        Method loadFromDefaultRootDir = EnvContextLoader.class.getDeclaredMethod("loadFromDefaultRootDirectory", String.class);
        loadFromDefaultRootDir.setAccessible(true);
        loadFromDefaultRootDir.invoke(envContextLoader, rootPath.toAbsolutePath().toString());

        Properties props = envContextLoader.getLoadedProperties();
            assertThat(props)
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(1);

            assertThat(props.stringPropertyNames())
                    .contains("KEY");

            assertThat(props.getProperty("KEY"))
                    .isEqualTo("VALUE");
    }

    @Test
    void whenDefaultRootLoaderIsTriggeredAndEnvFileIsNotAvailable_thenNothingIsLoaded() throws
                URISyntaxException, IOException, NoSuchMethodException, SecurityException, IllegalAccessException,
                InvocationTargetException {
        Path rootPath = Files.createDirectories(tempDir.resolve("springcontext-env"));

        EnvContextLoader envContextLoader = new EnvContextLoader();
        Method loadFromDefaultRootDir = EnvContextLoader.class.getDeclaredMethod("loadFromDefaultRootDirectory", String.class);
        loadFromDefaultRootDir.setAccessible(true);
        loadFromDefaultRootDir.invoke(envContextLoader, rootPath.toAbsolutePath().toString());

        Properties props = envContextLoader.getLoadedProperties();
            assertThat(props)
                    .isNotNull()
                    .isEmpty();
    }
}