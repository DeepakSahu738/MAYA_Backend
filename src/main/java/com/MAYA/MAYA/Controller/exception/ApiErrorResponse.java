package com.MAYA.MAYA.Controller.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApiErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;

    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(status, error, message, LocalDateTime.now());
    }
}
