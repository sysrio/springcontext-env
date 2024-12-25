package io.sysr.springcontext.env;

import org.springframework.boot.SpringApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

class SpringContextEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("springcontext-env");
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());
    }

    @Test
    void whenSpringContextEnvironmentPostProcessorIsInvoked_thenAvailableDotEnvFilesAreLoaded() throws IOException {
        String content = "KEY1=VALUE1\nKEY2=VALUE2\nKEY3=VALUE3";
        Files.writeString(tempDir.resolve(".env"), content, StandardCharsets.UTF_8);

        // Create a mock SpringApplication
        SpringApplication application = new SpringApplication();

        ConfigurableEnvironment environment = new StandardEnvironment();

        EnvironmentPostProcessor postProcessor = new SpringContextEnvironmentPostProcessor();

        postProcessor.postProcessEnvironment(environment, application);

        PropertySource<?> propertySources = environment.getPropertySources().get("springContextDotEnv");

        assertThat(propertySources).isNotNull();

        @SuppressWarnings("null")
        Properties props = (Properties) propertySources.getSource();
        assertThat(props).isNotNull()
                .hasFieldOrProperty("KEY1")
                .hasFieldOrProperty("KEY2")
                .hasFieldOrProperty("KEY3")
                .hasFieldOrPropertyWithValue("KEY1", "VALUE1")
                .hasFieldOrPropertyWithValue("KEY2", "VALUE2")
                .hasFieldOrPropertyWithValue("KEY3", "VALUE3");
    }
}
