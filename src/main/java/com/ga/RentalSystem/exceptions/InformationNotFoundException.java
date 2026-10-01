package com.ga.RentalSystem.exceptions;

public class InformationNotFoundException extends RuntimeException{
    public InformationNotFoundException(String message){
        super(message);
    }
}
