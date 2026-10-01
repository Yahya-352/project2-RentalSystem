package com.ga.RentalSystem.exceptions;

public class ConflictException extends RuntimeException {
    public ConflictException(String message){
        super(message);
    }
}
