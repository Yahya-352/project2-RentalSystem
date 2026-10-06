package com.ga.RentalSystem.exceptions;

import com.ga.RentalSystem.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InformationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(InformationNotFoundException ex, HttpServletRequest request) {
        return build(ex.getMessage(), HttpStatus.NOT_FOUND, request);
        //404 .. the thing doesnt exist
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
        //returns 400 ..something invalid has been sent
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(ex.getMessage(), HttpStatus.CONFLICT, request);
        //409 ..when we have a conflict with a thing that is already there(duplicates emails)
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return build("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR, request);
        //general...500
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(ex.getMessage(), HttpStatus.FORBIDDEN, request);
        //403..business rule fails..owner tries to update another owner car..thrown from my code
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build("You do not have permission to perform this action", HttpStatus.FORBIDDEN, request);
    }//403..role check fails..comes from spring security..built in

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        String message = "";

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            message = message + error.getField() + ": " + error.getDefaultMessage() + ". ";
        }

        return build(message.trim(), HttpStatus.BAD_REQUEST, request);
        //400 ..validate request from user (json) ..it will get the error message from notBlank in
        // dTO for example()
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(HandlerMethodValidationException ex,
                                                               HttpServletRequest request) {
        String message = "";

        for (MessageSourceResolvable error : ex.getAllErrors()) {
            message = message + error.getDefaultMessage() + ". ";
        }

        return build(message.trim(), HttpStatus.BAD_REQUEST, request);
        //validate path variable from user .. does same thing..returns the annotation message for request params
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<ErrorResponse> handleNotAuthorized(NotAuthorizedException ex, HttpServletRequest request) {
        return build(ex.getMessage(), HttpStatus.UNAUTHORIZED, request);
        //401 ..we dont know who you are (wrong email or password)
    }

    private ResponseEntity<ErrorResponse> build(String message, HttpStatus status, HttpServletRequest request) {
        String errorCode = status.name();

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                errorCode,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(errorResponse);
    }
}