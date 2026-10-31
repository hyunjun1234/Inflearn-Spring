package com.ai.shared.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.ConnectException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex){
        return ex.getMessage();
    }
    @ExceptionHandler(ConnectException.class)
    public String handleConnectException(Exception ex){
        return ex.getMessage();
    }
}
