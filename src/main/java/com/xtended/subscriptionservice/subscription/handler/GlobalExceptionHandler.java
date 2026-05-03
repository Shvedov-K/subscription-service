package com.xtended.subscriptionservice.subscription.handler;

import com.xtended.subscriptionservice.subscription.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/**
 * Глобальный обработчик исключений для REST контроллеров
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, ex.getMessage()));
    }

    /**
     * Обработка EntityNotFoundException
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, ex.getMessage()));
    }

    /**
     * Обработка IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, ex.getMessage()));
    }

    /**
     * Обработка SQLException
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponse<Void>> handleDbException(SQLException ex) {
        log.error(ex.getMessage());
        return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, ex.getMessage()));
    }
}
