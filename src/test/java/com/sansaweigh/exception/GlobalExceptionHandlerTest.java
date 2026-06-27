package com.sansaweigh.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    @DisplayName("BusinessException → 422 UNPROCESSABLE_ENTITY")
    void testHandleBusinessException() {
        BusinessException ex = new BusinessException("Night restriction violated");
        ResponseEntity<Map<String, Object>> resp = handler.handleBusinessException(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).containsEntry("message", "Night restriction violated");
        assertThat(resp.getBody()).containsKey("timestamp");
        assertThat(resp.getBody()).containsEntry("status", 422);
    }

    @Test
    @DisplayName("IllegalWeighingStateException → 400 BAD_REQUEST")
    void testHandleIllegalState() {
        IllegalWeighingStateException ex = new IllegalWeighingStateException("Bad transition");
        ResponseEntity<Map<String, Object>> resp = handler.handleIllegalState(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsEntry("message", "Bad transition");
        assertThat(resp.getBody()).containsEntry("status", 400);
    }

    @Test
    @DisplayName("ResourceNotFoundException → 404 NOT_FOUND")
    void testHandleNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Pesaje not found: abc");
        ResponseEntity<Map<String, Object>> resp = handler.handleNotFound(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("message", "Pesaje not found: abc");
        assertThat(resp.getBody()).containsEntry("status", 404);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with field errors concatenated")
    void testHandleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "scaleId", "scaleId is required");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().get("message").toString()).contains("scaleId");
    }

    @Test
    @DisplayName("Generic Exception → 500 INTERNAL_SERVER_ERROR")
    void testHandleGeneral() {
        Exception ex = new RuntimeException("Unexpected error");
        ResponseEntity<Map<String, Object>> resp = handler.handleGeneral(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().get("message").toString()).contains("Unexpected error");
        assertThat(resp.getBody()).containsEntry("status", 500);
    }

    @Test
    @DisplayName("Response body contains 'error' field with reason phrase")
    void testResponseBodyContainsError() {
        BusinessException ex = new BusinessException("test");
        ResponseEntity<Map<String, Object>> resp = handler.handleBusinessException(ex);

        assertThat(resp.getBody()).containsKey("error");
        assertThat(resp.getBody().get("error")).isEqualTo("Unprocessable Entity");
    }
}
