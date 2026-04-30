package com.clinica.agendamento.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) { }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ApiError ofValidation(String message, String path, List<FieldError> fields) {
        return new ApiError(LocalDateTime.now(), 400, "Bad Request", message, path, fields);
    }
}
