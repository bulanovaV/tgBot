package com.flightwatcher.common.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(Long id) {
        super("Alert with id = " + id + " not found");
    }
}
