package io.sysr.springcontext.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * The {@code SpringContextEnvironmentPostProcessor} class is an implementation
 * of {@link EnvironmentPostProcessor} and {@link Ordered} interfaces. It is
 * responsible for loading custom environment properties into the Spring
 * application context before the application is fully initialized.
 *
 * <p>
 * This post-processor utilizes the {@link EnvContextLoader} to load environment
 * variables from <code>.env</code> files file and adds them to the
 * application's {@link ConfigurableEnvironment} as a {@link PropertySource}.
 *
 * <p>
 * <b> Example usage: </b>
 * </p>
 * 
 * <pre>{@code
 * // In the Spring Boot application, this post-processor will automatically
 * // load and process the custom environment variables.
 * }</pre>
 *
 * <p>
 * This class must be registered in the {@code META-INF/spring.factories} file
 * to be recognized by Spring Boot.
 * </p>
 *
 * <pre>
 * org.springframework.boot.env.EnvironmentPostProcessor=\
 *   io.sysr.springcontext.env.SpringContextEnvironmentPostProcessor
 * </pre>
 *
 * <p>
 * The loading order of this post-processor is set to
 * {@code Ordered.HIGHEST_PRECEDENCE} to ensure that it runs before other
 * processors, ensuring that the loaded environment variables are available
 * early in the application context.
 * </p>
 *
 * @author Calvince Otieno
 * @version 1.0.0
 * @since 2024
 */
public class SpringContextEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * Default constructor
     */
    public SpringContextEnvironmentPostProcessor() {
        super();
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        EnvContextLoader loader = new EnvContextLoader();
        loader.load();

        PropertySource<?> propertySource = new PropertiesPropertySource("springContextDotEnv",
                loader.getLoadedProperties());
        environment.getPropertySources().addLast(propertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
