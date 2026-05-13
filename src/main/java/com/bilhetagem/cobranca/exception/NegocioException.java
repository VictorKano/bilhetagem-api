package com.bilhetagem.cobranca.exception;

/**
 * Exceção de negócio lançada quando uma regra de negócio é violada.
 * Mapeada para HTTP 422 Unprocessable Entity com corpo {codigo, mensagem}.
 */
public class NegocioException extends RuntimeException {

    private final String codigo;
    private final String mensagem;

    public NegocioException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
        this.mensagem = mensagem;
    }

    public NegocioException(String mensagem) {
        super(mensagem);
        this.codigo = "ERRO_NEGOCIO";
        this.mensagem = mensagem;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getMensagem() {
        return mensagem;
    }
}
