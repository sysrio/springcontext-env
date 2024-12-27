package io.sysr.springcontext.env.exception;

/**
 * Exception thrown when there is an error loading the environment variables
 * from a dotenv file in a Spring application. This exception extends
 * {@code RuntimeException}, indicating it is a unchecked exception that can
 * occur during the normal operation of the application, particularly during
 * environment setup or context loading.
 *
 * <p>
 * Using this exception helps to pinpoint issues related to the application
 * context not being properly configured due to missing or misconfigured
 * environment variables.
 * </p>
 *
 * <p>
 * <b> Example usage: </b>
 * </p>
 * 
 * <pre>
 * try {
 *     // Some code
 * } catch (IOException e) {
 *     throw new EnvContextLoaderException("Failed to load environment variables", e);
 * }
 * </pre>
 *
 * @author Calvince Otieno
 * @version 1.0.0
 * @since 2024
 */
public class EnvContextLoaderException extends RuntimeException {
    /**
     * Constructs a new EnvContextLoaderException with the specified detail message.
     *
     * @param message the detail message, saved for later retrieval by the
     *                {@link #getMessage()} method.
     */
    public EnvContextLoaderException(String message) {
        super(message);
    }

    /**
     * Constructs a new EnvContextLoaderException with the specified detail message
     * and cause.
     *
     * @param message the detail message explaining the reason for the exception,
     *                saved for later retrieval by the {@link #getMessage()} method.
     * @param cause   the underlying cause of the exception,
     *                saved for later retrieval by the {@link #getCause()} method.
     */
    public EnvContextLoaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
