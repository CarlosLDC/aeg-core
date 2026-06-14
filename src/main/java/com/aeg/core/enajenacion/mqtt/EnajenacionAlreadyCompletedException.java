package com.aeg.core.enajenacion.mqtt;

public class EnajenacionAlreadyCompletedException extends RuntimeException {

    public EnajenacionAlreadyCompletedException(String message) {
        super(message);
    }
}
