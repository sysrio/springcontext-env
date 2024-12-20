package io.sysr.springcontext.env;

import static org.assertj.core.api.Assertions.assertThat;

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
        URL url = getClass().getResource("/");
        Path root = Path.of(url.getPath());

        // Create the resources directory and env.properties file
        Path resourcesDir = root.resolve("resources");
        Files.createDirectories(resourcesDir);
        Path envFile = resourcesDir.resolve("env.properties");
        Files.createFile(envFile);

        try {
            // Instantiate the EnvContextLoader and access the private method
            EnvContextLoader envContextLoader = new EnvContextLoader();
            Method method = EnvContextLoader.class.getDeclaredMethod("getEnvConfigurationFilePath");

            method.setAccessible(true);
            String path = (String) method.invoke(envContextLoader);

            // Assertions to verify the method's output
            assertThat(path).isNotNull().endsWith("env.properties");
            assertThat(new File(path)).exists().isFile();
            assertThat(path).isEqualTo(envFile.toString());
        } finally {
            // Clean up resources
            Files.deleteIfExists(envFile);
            Files.deleteIfExists(resourcesDir);
        }
    }

    @Test
    void whenEnvPropertiesFileNotFound_thenMethodReturnsNull() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, SecurityException {
        EnvContextLoader envContextLoader = new EnvContextLoader();
        Method method = EnvContextLoader.class.getDeclaredMethod("getEnvConfigurationFilePath");
        method.setAccessible(true);
        String path = (String) method.invoke(envContextLoader);

        assertThat(path).isNull();
    }
}
