package com.flightwatcher.common.exception;

import com.flightwatcher.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AlertNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse alertNotFound(AlertNotFoundException exc){
        return new ErrorResponse(exc.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse argNotValid(MethodArgumentNotValidException exc){
        List<FieldError> err = exc.getBindingResult().getFieldErrors();
        StringBuilder sb = new StringBuilder();
        for (FieldError er: err){
            sb.append(er.getField()).append(" ").append(er.getDefaultMessage()).append("; ");
        }
        return new ErrorResponse(sb.toString());
    }
}