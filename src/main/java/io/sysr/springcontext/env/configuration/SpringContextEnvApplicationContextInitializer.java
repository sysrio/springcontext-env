package io.sysr.springcontext.env.configuration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.NonNull;

import io.sysr.springcontext.env.EnvContextLoader;

/**
 * A spring {@link ApplicationContextInitializer} that loads environment
 * variables from a <code>.env</code> files into the Spring application
 * context's environment. This initializer enhances Spring's configuration
 * by injecting .env file contents as additional properties. It makes
 * makes use of {@link EnvContextLoader} to read and load any provided
 * <code>.env (dotenv) </code> files.
 * <p>
 * This class ensures that when a Spring application starts, it can
 * automatically load environment variables from a any exisiting
 * <code>.env</code> file(s), making them available through Spring's property
 * resolution system.
 * </p>
 *
 * @author Calvince Otieno
 * @version 1.0.0
 * @since 2024
 */
public class SpringContextEnvApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    /**
     * Initializes the Spring application context by loading properties from the
     * available .env file using {@link EnvContextLoader}.
     * <p>
     * This method performs the following steps:
     * <ol>
     * <li>Instantiates an {@link EnvContextLoader} to handle the .env file
     * reading.</li>
     * <li>Calls {@link EnvContextLoader#load()} to load the properties from the
     * .env file.</li>
     * <li>Creates a new {@link PropertySource} with the loaded properties.</li>
     * <li>Adds this property source to the application context's environment.</li>
     * </ol>
     * </p>
     * 
     * @param applicationContext the {@link ConfigurableApplicationContext} to
     *                           initialize
     * @see EnvContextLoader
     * @see EnvContextLoader#load()
     * @see ConfigurableApplicationContext#getEnvironment()
     * @see PropertySource
     */
    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
        EnvContextLoader loader = new EnvContextLoader();
        loader.load();

        PropertySource<?> propertySource = new PropertiesPropertySource("springContextDotEnv",
                loader.getLoadedProperties());

        applicationContext.getEnvironment().getPropertySources().addLast(propertySource);
    }
}
