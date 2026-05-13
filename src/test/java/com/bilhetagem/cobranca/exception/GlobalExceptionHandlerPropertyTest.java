package com.bilhetagem.cobranca.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerPropertyTest {

    @Test
    void negocioExceptionRetorna422ComCodigoEMensagem() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        NegocioException ex = new NegocioException("ERR_001", "mensagem de erro");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleNegocioException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().codigo()).isEqualTo("ERR_001");
        assertThat(response.getBody().mensagem()).isEqualTo("mensagem de erro");
    }

    @Test
    void negocioExceptionComCamposVaziosRetorna422() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        NegocioException ex = new NegocioException("", "");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleNegocioException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().codigo()).isEqualTo("");
        assertThat(response.getBody().mensagem()).isEqualTo("");
    }

    @Test
    void violacoesValidacaoRetorna400ComListaDeErros() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> fieldErrors = List.of(
                new FieldError("obj", "campo1", "msg1"),
                new FieldError("obj", "campo2", "msg2")
        );
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                handler.handleMethodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errors()).hasSize(2);
        assertThat(response.getBody().errors().get(0).campo()).isEqualTo("campo1");
        assertThat(response.getBody().errors().get(0).mensagem()).isEqualTo("msg1");
        assertThat(response.getBody().errors().get(1).campo()).isEqualTo("campo2");
        assertThat(response.getBody().errors().get(1).mensagem()).isEqualTo("msg2");
    }

    @Test
    void violacoesValidacaoComUmItemRetorna400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "valor", "obrigatório")));

        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                handler.handleMethodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().get(0).campo()).isEqualTo("valor");
    }

    static Stream<Exception> excecoesDiversas() {
        return Stream.of(
                new RuntimeException(),
                new IllegalArgumentException(),
                new IllegalStateException(),
                new NullPointerException(),
                new UnsupportedOperationException(),
                new RuntimeException("com.bilhetagem.cobranca.service.CobrancaService: erro interno"),
                new RuntimeException("java.lang.NullPointerException at line 42"),
                new RuntimeException("org.springframework.dao.DataAccessException: connection refused"),
                new IllegalStateException("at com.bilhetagem.cobranca.controller.CobrancaController.criarCobranca(CobrancaController.java:45)"),
                new RuntimeException("erro inesperado"),
                new RuntimeException("erro externo", new IllegalStateException("causa interna")),
                new RuntimeException((String) null)
        );
    }

    @ParameterizedTest
    @MethodSource("excecoesDiversas")
    void excecaoNaoTratadaRespostaNaoExpoeInternos(Exception excecao) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/cobrancas");
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        ResponseEntity<GlobalExceptionHandler.InternalErrorResponse> response =
                handler.handleException(excecao, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();

        String mensagem = response.getBody().mensagem();
        assertThat(mensagem).isEqualTo("Erro interno do servidor");
        assertThat(mensagem).doesNotContain("\tat ", "at com.", "at java.", "at org.", "at sun.");
        assertThat(mensagem).doesNotContain("com.bilhetagem", "java.lang", "java.io", "org.springframework");
        assertThat(mensagem).doesNotContain(".java");
        assertThat(mensagem).doesNotContain("Exception", "Error");
    }
}
