package io.sysr.springcontext.env;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import io.sysr.springcontext.env.exception.EnvContextLoaderException;

import java.io.*;

public class EnvContextLoaderExceptionTest {

    @Test
    public void testExceptionWithMessage() {
        EnvContextLoaderException exception = new EnvContextLoaderException("Error loading environment");
        assertNotNull(exception);
        assertEquals("Error loading environment", exception.getMessage());
    }

    @Test
    public void testExceptionWithMessageAndCause() {
        Throwable cause = new Throwable("Root cause");
        EnvContextLoaderException exception = new EnvContextLoaderException("Error loading environment", cause);
        assertNotNull(exception);
        assertEquals("Error loading environment", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testExceptionWithNullMessage() {
        @SuppressWarnings("null")
        EnvContextLoaderException exception = new EnvContextLoaderException(null);
        assertNull(exception.getMessage());
    }

    @Test
    public void testExceptionWithNullCause() {
        EnvContextLoaderException exception = new EnvContextLoaderException("Error loading environment", null);
        assertNotNull(exception);
        assertEquals("Error loading environment", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testExceptionWithNullMessageAndCause() {
        @SuppressWarnings("null")
        EnvContextLoaderException exception = new EnvContextLoaderException(null, null);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testExceptionWithSuppressedExceptions() {
        EnvContextLoaderException exception = new EnvContextLoaderException("Error loading environment");
        exception.addSuppressed(new IllegalArgumentException("Suppressed exception"));
        assertEquals(1, exception.getSuppressed().length);
        assertEquals("Suppressed exception", exception.getSuppressed()[0].getMessage());
    }

    @Test
    public void testExceptionStackTraceNotEmpty() {
        EnvContextLoaderException exception = new EnvContextLoaderException("Error");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testExceptionIsSerializable() throws IOException, ClassNotFoundException {
        EnvContextLoaderException exception = new EnvContextLoaderException("Error loading environment");
        // Serialize the exception
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(exception);

        // Deserialize the exception
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        EnvContextLoaderException deserializedException = (EnvContextLoaderException) ois.readObject();

        assertEquals(exception.getMessage(), deserializedException.getMessage());
    }
}
