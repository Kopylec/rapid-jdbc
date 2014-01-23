package com.github.kopylec.rapidjdbc.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The only exception that can be thrown by RapidJDBC.
 *
 * @author Mariusz Kopylec
 * @since 1.0
 */
public class RapidJDBCException extends RuntimeException {

    private static final Logger LOGGER = LoggerFactory.getLogger(RapidJDBCException.class);

    public RapidJDBCException(String message) {
        super(message);
        logError(message);
    }

    public RapidJDBCException(String message, Throwable cause) {
        super(message, cause);
        logError(message, cause);
    }

    private void logError(String message) {
        LOGGER.error("{}. An exception will be thrown.", message);
    }

    private void logError(String message, Throwable cause) {
        LOGGER.error("{}. {}. An exception will be thrown.", message, cause.getMessage());
    }

}
