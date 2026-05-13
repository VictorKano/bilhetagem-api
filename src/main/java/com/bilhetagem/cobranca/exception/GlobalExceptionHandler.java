package com.bilhetagem.cobranca.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Tratador global de exceções para a API de cobranças.
 * Mapeia exceções de negócio e de infraestrutura para respostas HTTP padronizadas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(String codigo, String mensagem) {}

    public record NotFoundResponse(String mensagem) {}

    public record ValidationErrorResponse(List<FieldErrorItem> errors) {
        public record FieldErrorItem(String campo, String mensagem) {}
    }

    public record InternalErrorResponse(String mensagem) {}

    /**
     * NegocioException → HTTP 422 com {codigo, mensagem}.
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> handleNegocioException(NegocioException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(ex.getCodigo(), ex.getMensagem()));
    }

    /**
     * LockNotAcquiredException → HTTP 422 com {codigo: "LOCK_NAO_ADQUIRIDO", mensagem: "Geração de cobrança em andamento"}.
     */
    @ExceptionHandler(LockNotAcquiredException.class)
    public ResponseEntity<ErrorResponse> handleLockNotAcquiredException(LockNotAcquiredException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("LOCK_NAO_ADQUIRIDO", "Geração de cobrança em andamento"));
    }

    /**
     * RecursoNaoEncontradoException → HTTP 404 com {mensagem}.
     */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<NotFoundResponse> handleRecursoNaoEncontradoException(RecursoNaoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new NotFoundResponse(ex.getMessage()));
    }

    /**
     * MethodArgumentNotValidException → HTTP 400 com lista de {campo, mensagem}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {

        List<ValidationErrorResponse.FieldErrorItem> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ValidationErrorResponse.FieldErrorItem(
                        fe.getField(),
                        fe.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse(fieldErrors));
    }

    /**
     * Exception (fallback) → HTTP 500 com {mensagem: "Erro interno do servidor"}.
     * Registra log ERROR com método HTTP, path e correlationId (se disponível no header X-Correlation-Id).
     * Não expõe stack trace na resposta.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<InternalErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String correlationId = request.getHeader("X-Correlation-Id");

        log.error("Erro interno não tratado — método: {}, path: {}, correlationId: {}",
                method, path, correlationId, ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new InternalErrorResponse("Erro interno do servidor"));
    }
}
