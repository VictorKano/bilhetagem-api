package com.bilhetagem.cobranca.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("NegocioException deve retornar HTTP 422 com codigo e mensagem corretos")
    void handleNegocioExceptionDeveRetornar422ComCodigoEMensagem() {
        NegocioException ex = new NegocioException("ERR_CODE", "msg");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleNegocioException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().codigo()).isEqualTo("ERR_CODE");
        assertThat(response.getBody().mensagem()).isEqualTo("msg");
    }

    @Test
    @DisplayName("LockNotAcquiredException deve retornar HTTP 422 com codigo LOCK_NAO_ADQUIRIDO")
    void handleLockNotAcquiredExceptionDeveRetornar422ComCodigoLockNaoAdquirido() {
        LockNotAcquiredException ex = new LockNotAcquiredException();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleLockNotAcquiredException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().codigo()).isEqualTo("LOCK_NAO_ADQUIRIDO");
        assertThat(response.getBody().mensagem()).isEqualTo("Geração de cobrança em andamento");
    }

    @Test
    @DisplayName("RecursoNaoEncontradoException deve retornar HTTP 404 com mensagem correta")
    void handleRecursoNaoEncontradoExceptionDeveRetornar404ComMensagem() {
        RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException("not found");
        ResponseEntity<GlobalExceptionHandler.NotFoundResponse> response =
                handler.handleRecursoNaoEncontradoException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensagem()).isEqualTo("not found");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException deve retornar HTTP 400 com lista de erros de campo")
    void handleMethodArgumentNotValidExceptionDeveRetornar400ComListaDeErros() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError("cobrancaRequestDTO", "valor",
                "deve ser maior que 0");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                handler.handleMethodArgumentNotValidException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().get(0).campo()).isEqualTo("valor");
        assertThat(response.getBody().errors().get(0).mensagem()).isEqualTo("deve ser maior que 0");
    }

    @Test
    @DisplayName("Exception genérica deve retornar HTTP 500 com mensagem genérica sem expor detalhes")
    void handleExceptionDeveRetornar500ComMensagemGenerica() {
        Exception ex = new Exception("unexpected");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);
        ResponseEntity<GlobalExceptionHandler.InternalErrorResponse> response =
                handler.handleException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensagem()).isEqualTo("Erro interno do servidor");
    }
}
