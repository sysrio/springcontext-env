package io.sysr.springcontext.env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvContextLoaderTest {
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
            InvocationTargetException, SecurityException, IOException {
        // Get the root URL from the class loader
        URL url = EnvContextLoader.class.getClassLoader().getResource("");
        Path root = Path.of(url.getPath());

        // Create the resources directory and env.properties file
        Path resourcesDirPath = root.resolve("resources");
        Files.createDirectories(resourcesDirPath);
        Path envFile = resourcesDirPath.resolve("env.properties");
        Files.createFile(envFile);

        try {
            EnvContextLoader envContextLoader = new EnvContextLoader();
            Method method = EnvContextLoader.class.getDeclaredMethod("findEnvPropertiesFile");
            method.setAccessible(true);
            String path = (String) method.invoke(envContextLoader);

            assertThat(path).isNotNull().endsWith("env.properties");
            assertThat(new File(path)).exists().isFile();
            assertThat(path).isEqualTo(envFile.toString());
        } finally {
            Files.deleteIfExists(envFile);
            Files.deleteIfExists(resourcesDirPath);
        }
    }

    @Test
    void whenEnvPropertiesFileNotFound_thenMethodReturnsNull() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, SecurityException {
        // Get the root URL from the class loader
        URL url = EnvContextLoader.class.getClassLoader().getResource("");
        Path root = Path.of(url.getPath());

        // Ensure the env.properties file does not exist
        Path resourcesDirPath = root.resolve("resources");
        Path envFile = resourcesDirPath.resolve("env.properties");

        if (Files.exists(envFile)) {
            fail("Test failed because env.properties file exists when it should not.");
        }

        EnvContextLoader envContextLoader = new EnvContextLoader();
        Method method = EnvContextLoader.class.getDeclaredMethod("findEnvPropertiesFile");
        method.setAccessible(true);
        String path = (String) method.invoke(envContextLoader);

        assertThat(path).isNull();
    }
}
