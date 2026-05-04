package com.xtended.subscriptionservice.subscription.handler;

import com.xtended.subscriptionservice.subscription.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("Должен обрабатывать IllegalArgumentException с статусом 400")
    void handleBadRequest_ShouldReturnBadRequestStatus_WhenIllegalArgumentExceptionThrown() {
        String errorMessage = "Некорректный аргумент";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleBadRequest(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Должен обрабатывать EntityNotFoundException с статусом 404")
    void handleNotFound_ShouldReturnNotFoundStatus_WhenEntityNotFoundExceptionThrown() {
        String errorMessage = "Сущность не найдена";
        EntityNotFoundException exception = new EntityNotFoundException(errorMessage);

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Должен обрабатывать IllegalStateException с статусом 400")
    void handleIllegalState_ShouldReturnBadRequestStatus_WhenIllegalStateExceptionThrown() {
        String errorMessage = "Некорректное состояние";
        IllegalStateException exception = new IllegalStateException(errorMessage);

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleIllegalState(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Должен обрабатывать SQLException с статусом 500")
    void handleDbException_ShouldReturnInternalServerErrorStatus_WhenSQLExceptionThrown() {
        String errorMessage = "Ошибка базы данных";
        SQLException exception = new SQLException(errorMessage);

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleDbException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Должен обрабатывать IllegalArgumentException с пустым сообщением")
    void handleBadRequest_ShouldHandleEmptyMessage_WhenIllegalArgumentExceptionThrown() {
        IllegalArgumentException exception = new IllegalArgumentException();

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleBadRequest(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Должен обрабатывать EntityNotFoundException с пустым сообщением")
    void handleNotFound_ShouldHandleEmptyMessage_WhenEntityNotFoundExceptionThrown() {
        EntityNotFoundException exception = new EntityNotFoundException();

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Должен обрабатывать IllegalStateException с пустым сообщением")
    void handleIllegalState_ShouldHandleEmptyMessage_WhenIllegalStateExceptionThrown() {
        IllegalStateException exception = new IllegalStateException();

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleIllegalState(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Должен обрабатывать SQLException с пустым сообщением")
    void handleDbException_ShouldHandleEmptyMessage_WhenSQLExceptionThrown() {
        SQLException exception = new SQLException();

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleDbException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertNotNull(response.getBody().getTimestamp());
    }


    @Test
    @DisplayName("Должен возвращать разные статусы для разных исключений")
    void handleExceptions_ShouldReturnDifferentStatuses_ForDifferentExceptions() {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Bad argument");
        EntityNotFoundException entityNotFoundException = new EntityNotFoundException("Not found");
        IllegalStateException illegalStateException = new IllegalStateException("Illegal state");
        SQLException sqlException = new SQLException("SQL error");

        ResponseEntity<ApiResponse<Void>> badRequestResponse = globalExceptionHandler.handleBadRequest(illegalArgumentException);
        ResponseEntity<ApiResponse<Void>> notFoundResponse = globalExceptionHandler.handleNotFound(entityNotFoundException);
        ResponseEntity<ApiResponse<Void>> illegalStateResponse = globalExceptionHandler.handleIllegalState(illegalStateException);
        ResponseEntity<ApiResponse<Void>> internalServerErrorResponse = globalExceptionHandler.handleDbException(sqlException);

        assertEquals(HttpStatus.BAD_REQUEST, badRequestResponse.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, illegalStateResponse.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, internalServerErrorResponse.getStatusCode());

        assertNotNull(badRequestResponse.getBody());
        assertNotNull(notFoundResponse.getBody());
        assertNotNull(illegalStateResponse.getBody());
        assertNotNull(internalServerErrorResponse.getBody());

        assertFalse(badRequestResponse.getBody().isSuccess());
        assertFalse(notFoundResponse.getBody().isSuccess());
        assertFalse(illegalStateResponse.getBody().isSuccess());
        assertFalse(internalServerErrorResponse.getBody().isSuccess());
    }
}
