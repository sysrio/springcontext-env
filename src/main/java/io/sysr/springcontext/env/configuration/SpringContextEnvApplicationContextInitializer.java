package io.sysr.springcontext.env.configuration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.NonNull;

import io.sysr.springcontext.env.EnvContextLoader;

/**
 * A Spring {@link ApplicationContextInitializer} that loads environment
 * variables from <code>.env</code> files into the Spring application
 * context's environment. This initializer enhances Spring's configuration
 * by injecting the contents of <code>.env</code> files as additional
 * properties.
 * It utilizes {@link EnvContextLoader} to read and load any provided
 * <code>.env</code> (dotenv) files.
 * <p>
 * This class ensures that when a Spring application starts, it can
 * automatically load environment variables from any existing
 * <code>.env</code> file(s), making them available through Spring's property
 * resolution system.
 * </p>
 *
 * <p>
 * The properties loaded from the <code>.env</code> file are added to the
 * Spring application context's environment, allowing for seamless integration
 * of environment-specific configurations.
 * </p>
 *
 * @author Calvince Otieno
 * @version 1.0.0
 * @since 2024
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpringContextEnvApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Default constructor for the class. Initializes a new instance of
     * {@link SpringContextEnvApplicationContextInitializer}.
     */
    public SpringContextEnvApplicationContextInitializer() {
        super();
    }

    /**
     * Initializes the Spring application context by loading properties from the
     * available <code>.env</code> file using {@link EnvContextLoader}.
     * <p>
     * This method instantiates an {@link EnvContextLoader} to handle the
     * reading of the <code>.env</code> file. It calls
     * {@link EnvContextLoader#load()} to load the properties from the
     * <code>.env</code> file. The loaded properties are then added to the
     * Spring application context's environment.
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
