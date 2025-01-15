package io.sysr.springcontext.env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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

import io.sysr.springcontext.env.exception.EnvContextLoaderException;

class EnvContextLoaderTest {
        private static final String ENV_PROPERTIES_CONFIG_FILE_NAME = "dotenv.properties";
        private EnvContextLoader envContextLoader;

        @TempDir
        Path tempDir;

        @BeforeEach
        void setUp() throws IOException {
                envContextLoader = new EnvContextLoader();
                tempDir = Files.createTempDirectory("springcontext-env");
                System.setProperty("user.dir", tempDir.toAbsolutePath().toString());
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

                // Create the env.properties file
                Path envFile = root.resolve(ENV_PROPERTIES_CONFIG_FILE_NAME);
                Files.createFile(envFile);

                try {
                        Method method = EnvContextLoader.class.getDeclaredMethod("findEnvPropertiesFile");
                        method.setAccessible(true);
                        String path = (String) method.invoke(envContextLoader);

                        assertThat(path).isNotNull().endsWith(ENV_PROPERTIES_CONFIG_FILE_NAME);
                        assertThat(new File(path)).exists().isFile();
                        assertThat(path).isEqualTo(envFile.toString());
                } finally {
                        Files.deleteIfExists(envFile);
                }
        }

        @Test
        void whenEnvPropertiesFileNotFound_thenMethodReturnsNull() throws NoSuchMethodException, IllegalAccessException,
                        InvocationTargetException, SecurityException, URISyntaxException {
                // Get the root URL from the class loader
                URL url = EnvContextLoader.class.getClassLoader().getResource("");
                Path root = Path.of(url.toURI());

                // Ensure the env.properties file does not exist
                Path envFile = root.resolve(ENV_PROPERTIES_CONFIG_FILE_NAME);

                if (Files.exists(envFile)) {
                        fail("Test failed because env.properties file exists when it should not.");
                }

                Method method = EnvContextLoader.class.getDeclaredMethod("findEnvPropertiesFile");
                method.setAccessible(true);
                String path = (String) method.invoke(envContextLoader);

                assertThat(path).isNull();
        }

        @Test
        void whenUserProvidedValidPropertiesFilePath_thenTheFileContentMustBeLoadedSuccessfully()
                        throws URISyntaxException,
                        IOException, NoSuchMethodException, SecurityException, IllegalAccessException,
                        InvocationTargetException {
                Path userDir = tempDir.resolve("config");
                Files.createDirectories(userDir);
                Path filePath = userDir.resolve("env");
                Files.createFile(filePath);
                Files.writeString(filePath, "KEY=VALUE", StandardCharsets.UTF_8);

                // Get the root URL from the class loader
                URL url = EnvContextLoader.class.getClassLoader().getResource("");
                Path root = Path.of(url.toURI());

                // Create the resources directory and env.properties file
                Path envFile = root.resolve(ENV_PROPERTIES_CONFIG_FILE_NAME);
                Files.createFile(envFile);
                Files.writeString(envFile,
                                "ENV_DIR_PATH=%s%nFILE_NAME=%s%n".formatted(
                                                userDir.toString().replace("\\", "\\\\"),
                                                filePath.getFileName().toString()));

                try {
                        Method findEnvPropertiesFileMethod = EnvContextLoader.class
                                        .getDeclaredMethod("findEnvPropertiesFile");

                        findEnvPropertiesFileMethod.setAccessible(true);
                        String path = (String) findEnvPropertiesFileMethod.invoke(envContextLoader);

                        assertThat(path).isNotNull().endsWith(ENV_PROPERTIES_CONFIG_FILE_NAME);
                        assertThat(new File(path)).exists().isFile();
                        assertThat(path).isEqualTo(envFile.toString());

                        // Load file from user dir
                        Method loadFromUserDirMethod = EnvContextLoader.class.getDeclaredMethod(
                                        "loadEnvFilesFromUserDefinedPath",
                                        String.class);

                        loadFromUserDirMethod.setAccessible(true);
                        loadFromUserDirMethod.invoke(envContextLoader, path);

                        Properties props = envContextLoader.getLoadedProperties();

                        assertThat(props).isNotNull().hasSize(1);
                        assertThat(props.stringPropertyNames()).contains("KEY");
                        assertThat(props.getProperty("KEY")).isEqualTo("VALUE");

                } finally {
                        Files.deleteIfExists(envFile);
                }
        }

        @Test
        void whenUserProvidedInvalidPropertiesFilePath_thenNothingIsLoaded() throws URISyntaxException, IOException,
                        NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {

                // Get the root URL from the class loader
                URL url = EnvContextLoader.class.getClassLoader().getResource("");
                Path root = Path.of(url.toURI());

                // Create the resouce file: env.properties file
                Path envFile = root.resolve(ENV_PROPERTIES_CONFIG_FILE_NAME);
                Files.createFile(envFile);
                Files.writeString(envFile,
                                "ENV_DIR_PATH=%s%nFILE_NAME=%s%n".formatted(
                                                tempDir.toString().replace("\\", "\\\\"),
                                                "nofile"));

                try {
                        Method findEnvPropertiesFileMethod = EnvContextLoader.class
                                        .getDeclaredMethod("findEnvPropertiesFile");

                        findEnvPropertiesFileMethod.setAccessible(true);
                        String path = (String) findEnvPropertiesFileMethod.invoke(envContextLoader);

                        assertThat(path).isNotNull().endsWith(ENV_PROPERTIES_CONFIG_FILE_NAME);
                        assertThat(new File(path)).exists().isFile();
                        assertThat(path).isEqualTo(envFile.toString());

                        // Load file from user dir
                        Method loadFromUserDirMethod = EnvContextLoader.class.getDeclaredMethod(
                                        "loadEnvFilesFromUserDefinedPath",
                                        String.class);

                        loadFromUserDirMethod.setAccessible(true);
                        loadFromUserDirMethod.invoke(envContextLoader, path);

                        Properties props = envContextLoader.getLoadedProperties();
                        assertThat(props).isNotNull().isEmpty();

                } finally {
                        Files.deleteIfExists(envFile);
                }
        }

        @Test
        void whenDefaultRootLoaderIsTriggeredAndEnvFileIsAvailable_thenTheFileContentMustBeLoadedSuccessfully()
                        throws IOException, NoSuchMethodException, SecurityException, IllegalAccessException,
                        InvocationTargetException {
                Path rootPath = tempDir;
                Path envFile = Files.createFile(rootPath.resolve(".env"));
                Files.writeString(envFile, "KEY=VALUE", StandardCharsets.UTF_8);

                Method loadFromDefaultRootDir = EnvContextLoader.class.getDeclaredMethod("loadEnvFilesFromDirectory",
                                String.class);
                loadFromDefaultRootDir.setAccessible(true);
                loadFromDefaultRootDir.invoke(envContextLoader, rootPath.toAbsolutePath().toString());

                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(1);
                assertThat(props.stringPropertyNames()).contains("KEY");
                assertThat(props.getProperty("KEY")).isEqualTo("VALUE");
        }

        @Test
        void whenDefaultRootLoaderIsTriggeredAndEnvFileIsNotAvailable_thenNothingIsLoaded()
                        throws NoSuchMethodException, SecurityException, IllegalAccessException,
                        InvocationTargetException {
                Method loadFromDefaultRootDir = EnvContextLoader.class.getDeclaredMethod("loadEnvFilesFromDirectory",
                                String.class);
                loadFromDefaultRootDir.setAccessible(true);
                loadFromDefaultRootDir.invoke(envContextLoader, tempDir.toAbsolutePath().toString());

                Properties props = envContextLoader.getLoadedProperties();
                assertThat(props).isNotNull().isEmpty();
        }

        @Test
        void whenEnvFileWithMultipleKeyValuesIsProvided_thenTheKeyValuePairsMustBeLoadedSuccessfully()
                        throws IOException {
                String content = "KEY1=VALUE1\nKEY2==VALUE2\nKEY3:VALUE3\nDESC=Some description";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(4)
                                .containsKeys("KEY1", "KEY2", "KEY3", "DESC")
                                .containsValues("VALUE1", "=VALUE2", "VALUE3", "Some description");
        }

        @Test
        void whenValidEnvVariableIsProvidedAndReferenced_thenEnvVariableShouldBeSubstituted() throws IOException {
                String content = "KEY1=VALUE1\nKEY2=${KEY1}";
                Files.writeString(tempDir.resolve(".env"), content);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(2)
                                .containsKeys("KEY1", "KEY2")
                                .containsValues("VALUE1", "VALUE1");
        }

        @Test
        void whenSystemEnvVariableIsProvidedAndReferenced_thenSubstituteEnvVariable() throws IOException {
                System.setProperty("SYSTEM_VAR", "SystemValue");
                String content = "KEY=${SYSTEM_VAR}\nPATH=\n";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(2)
                                .containsKeys("KEY", "PATH")
                                .containsValues("SystemValue", System.getenv("PATH"));
        }

        @Test
        void whenUndefinedVariableIsReferenced_thenEnvVariableShouldBeSkipped()
                        throws IOException {
                String content = "KEY=${UNDEFINED_VAR}";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();
                assertThat(props).isNotNull().isEmpty();
        }

        @Test
        void whenCircularDependencyIsDetected_thenThrowEnvContextLoaderException() throws IOException {
                String content = "KEY1=${KEY2}\nKEY2=${KEY1}";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                Throwable thrown = catchThrowable(() -> envContextLoader.load());

                assertThat(thrown).isInstanceOf(EnvContextLoaderException.class)
                                .hasMessageContaining("Circular dependency detected on variable KEY2.");
        }

        @Test
        void whenVariableIsSelfReferencing_thenThrowEnvContextLoaderExceptionForCircularDependency()
                        throws IOException {
                String content = "KEY=${KEY}";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                Throwable thrown = catchThrowable(() -> envContextLoader.load());

                assertThat(thrown).isInstanceOf(EnvContextLoaderException.class)
                                .hasMessageContaining("Circular dependency detected on variable KEY.");
        }

        @Test
        void whenMultipleEnvFilesAreProvided_thenTheyMustBeLoadedSuccessfully() throws IOException {
                Files.writeString(tempDir.resolve(".env"), "KEY1=VALUE1", StandardCharsets.UTF_8);
                Files.writeString(tempDir.resolve(".env.dev"), "KEY2=VALUE2", StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(2)
                                .containsKeys("KEY1", "KEY2")
                                .containsValues("VALUE1", "VALUE2");
        }

        @Test
        void whenEnvAndSystemVarsAreDefined_thenEnvVarTakesPrecedence() throws IOException {
                System.setProperty("KEY", "SystemValue");
                String content = "KEY=EnvValue";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().isNotEmpty()
                                .containsKey("KEY")
                                .containsValue("EnvValue");
        }

        @Test
        void whenVariableValueIsHavingLeadingOrTrailingSpaces_thenTheyShouldBeRemoved() throws IOException {
                String content = "KEY= VALUE \nKEY2 \t = \t VALUE2";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().isNotEmpty()
                                .containsKeys("KEY", "KEY2")
                                .containsValues("VALUE", "VALUE2");
        }

        @Test
        void whenEnvFileIsEmpty_thenNothingIsLoaded() throws IOException {
                Files.writeString(tempDir.resolve(".env"), "", StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().isEmpty();
        }

        @Test
        void whenEnvFileContainsComments_thenTheCommentsShouldBeSkipped() throws IOException {
                String content = "# This is a comment\nKEY=VALUE";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(1)
                                .containsKey("KEY")
                                .containsValue("VALUE");
        }

        @Test
        void whenEnvFileIsContainingAnInvalidLine_thenThatLineShouldBeSkipped() throws IOException {
                String content = "INVALID_LINE\nKEY=VALUE\nIVB\nPAIR=VAL";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(2)
                                .containsKeys("KEY", "PAIR")
                                .containsValues("VALUE", "VAL");
        }

        @Test
        void whenEnvFileContainsUnicodeCharacters_thenTheCaractersShouldBeLoadedSuccessfully() throws IOException {
                String content = "KEY=价值";
                Files.writeString(tempDir.resolve(".env"), content,
                                StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(1)
                                .containsKey("KEY")
                                .containsValue("价值");
        }

        @Test
        void whenSubstitutingMultipleEnvVariables_thenReferencesAreCorrectlyResolved() throws IOException {
                String content = "USERNAME=jdoe\nHOST=sysr.io\nPASSWORD=Passw0rd\nURL=https://${HOST}:2024/${USERNAME}:${PASSWORD}";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(4)
                                .containsKeys("USERNAME", "HOST", "PASSWORD", "URL")
                                .containsValues("jdoe", "sysr.io", "Passw0rd", "https://sysr.io:2024/jdoe:Passw0rd");

                assertThat(props.getProperty("USERNAME")).isEqualTo("jdoe");
                assertThat(props.getProperty("HOST")).isEqualTo("sysr.io");
                assertThat(props.getProperty("PASSWORD")).isEqualTo("Passw0rd");
                assertThat(props.getProperty("URL")).isEqualTo("https://sysr.io:2024/jdoe:Passw0rd");
        }

        @Test
        void whenEnvVariablesAreLoaded_thenCaseSensitivityMustBeMaintained() throws IOException {
                String content = "Key=VALUE\nKEY=value";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(2)
                                .containsKeys("Key", "KEY")
                                .containsValues("VALUE", "value");

                assertThat(props.getProperty("Key")).isEqualTo("VALUE");
                assertThat(props.getProperty("KEY")).isEqualTo("value");
        }

        @Test
        void whenEnvVariablesHaveSpecialCharacters_thenTheyAreHandledCorrectly() throws IOException {
                String content = "KEY-1=VALUE`.1\nKEY_2=VALUE2!\n_KEY=valid$";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(3)
                                .containsKeys("KEY-1", "KEY_2", "_KEY")
                                .containsValues("VALUE`.1", "VALUE2!", "valid$");
                assertThat(props.getProperty("KEY-1")).isEqualTo("VALUE`.1");
                assertThat(props.getProperty("KEY_2")).isEqualTo("VALUE2!");
                assertThat(props.getProperty("_KEY")).isEqualTo("valid$");

        }

        @Test
        void whenMalformedVariableDefinitions_thenTheyAreIgnored() throws IOException {
                String content = "KEY=${UNFINISHED_VAR\nEMPTY=${}\nBAD=${\nNOTBAD=$}\nEMPTY_WHITESPACE=${    }";
                Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(1);
        }

        @Test
        void whenHandlingLargeNumberOfVariables_thenAllVariablesAreLoadedSuccessfully() throws IOException {
                // Arrange: Create a large .env file with 1000 key-value pairs
                StringBuilder content = new StringBuilder();
                for (int i = 0; i < 10000; i++) {
                        content.append("KEY").append(i).append("=VALUE").append(i).append("\n");
                }
                Files.writeString(tempDir.resolve(".env"), content.toString());

                envContextLoader.load();
                Properties props = envContextLoader.getLoadedProperties();

                assertThat(props).isNotNull().hasSize(10000);
                assertThat(props.getProperty("KEY1")).isEqualTo("VALUE1");
                assertThat(props.getProperty("KEY1000")).isEqualTo("VALUE1000");
                assertThat(props.getProperty("KEY9999")).isEqualTo("VALUE9999");
        }
}