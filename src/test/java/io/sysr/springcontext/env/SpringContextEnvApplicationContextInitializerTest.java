package io.sysr.springcontext.env;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import io.sysr.springcontext.env.configuration.SpringContextEnvApplicationContextInitializer;

class SpringContextEnvApplicationContextInitializerTest {
    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createDirectories(tempDir.resolve("springcontext-env"));
        String content = "KEY1=VALUE1\nKEY2=VALUE2\nKEY3=Some-${KEY1}";
        Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());
    }

    @Test
    @SuppressWarnings("null")
    void whenSpringContextEnvApplicationContextInitializerIsInvoked_thenAvailableDotEnvFilesAreLoaded() {
        // Create application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // Manually apply the initializer
        ApplicationContextInitializer<ConfigurableApplicationContext> initializer = new SpringContextEnvApplicationContextInitializer();
        initializer.initialize(context);

        ConfigurableEnvironment environment = context.getEnvironment();

        // Check if the property source exists
        PropertySource<?> propertySource = environment.getPropertySources().get("springContextDotEnv");
        assertThat(propertySource).isNotNull();

        Properties props = (Properties) propertySource.getSource();
        assertThat(props).isNotNull()
                .hasFieldOrProperty("KEY1")
                .hasFieldOrProperty("KEY2")
                .hasFieldOrProperty("KEY3")
                .hasFieldOrPropertyWithValue("KEY1", "VALUE1")
                .hasFieldOrPropertyWithValue("KEY2", "VALUE2")
                .hasFieldOrPropertyWithValue("KEY3", "Some-VALUE1");

        assertThat(environment.getProperty("KEY1")).isEqualTo("VALUE1");
        assertThat(environment.getProperty("KEY2")).isEqualTo("VALUE2");
        assertThat(environment.getProperty("KEY3")).isEqualTo("Some-VALUE1");
    }

}