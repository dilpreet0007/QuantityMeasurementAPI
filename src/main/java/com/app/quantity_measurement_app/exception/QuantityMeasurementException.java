package com.app.quantity_measurement_app.exception;



@SuppressWarnings("serial")
public class QuantityMeasurementException extends RuntimeException {

    /**
     * Constructs a new QuantityMeasurementException with the specified detail message.
     * @param message the detail message explaining the reason for the exception
     */
    public QuantityMeasurementException(String message) {
        super(message);
    }

    /**
     * Constructs a new QuantityMeasurementException with the specified detail message 
     * and cause.
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public QuantityMeasurementException(String message, Throwable cause) {
        super(message, cause);
    }
}