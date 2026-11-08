package com.ga.car_service.exceptions;

public class ConflictException extends RuntimeException {
    public ConflictException(String message){
        super(message);
    }
}
