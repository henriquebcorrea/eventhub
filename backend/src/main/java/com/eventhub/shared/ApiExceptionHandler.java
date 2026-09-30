package com.eventhub.shared;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException exception) {
        var problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
        problem.setTitle(exception.code());
        problem.setType(URI.create("https://eventhub.dev/problems/" + exception.code().toLowerCase()));
        problem.setProperty("code", exception.code());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revise os campos informados.");
        problem.setTitle("VALIDATION_ERROR");
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty("fields", exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage() == null ? "inválido" : e.getDefaultMessage(), (a, b) -> a)));
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraint(ConstraintViolationException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("VALIDATION_ERROR");
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConflict(DataIntegrityViolationException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A operação conflita com um registro existente.");
        problem.setTitle("DATA_CONFLICT");
        problem.setProperty("code", "DATA_CONFLICT");
        return problem;
    }
}

