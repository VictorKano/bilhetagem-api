package com.bilhetagem.cobranca.service.strategy;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;

/**
 * Strategy para criação de cobrança por método de pagamento.
 * Cada implementação é responsável por invocar o gateway adequado
 * e preencher os campos específicos da entidade {@link Cobranca}.
 */
public interface CobrancaCriacaoStrategy {

    /**
     * Retorna o método de pagamento que esta estratégia suporta.
     *
     * @return o {@link CobrancaMetodoEnum} correspondente
     */
    CobrancaMetodoEnum getMetodo();

    /**
     * Executa a criação da cobrança no gateway e preenche os campos
     * específicos do método de pagamento na entidade.
     *
     * @param cobranca entidade já inicializada com dados básicos
     * @param request  dados da requisição de criação
     */
    void executar(Cobranca cobranca, CobrancaRequestDTO request);
}
