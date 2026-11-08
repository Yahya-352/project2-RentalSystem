package com.ga.user_service.exceptions;

public class ConflictException extends RuntimeException {
    public ConflictException(String message){
        super(message);
    }
}
