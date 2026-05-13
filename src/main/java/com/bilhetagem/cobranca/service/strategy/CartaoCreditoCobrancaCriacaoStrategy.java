package com.bilhetagem.cobranca.service.strategy;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;
import com.bilhetagem.cobranca.integration.GatewayCartaoRequest;
import com.bilhetagem.cobranca.integration.GatewayCartaoResponse;
import com.bilhetagem.cobranca.integration.PagamentoGatewayClient;
import org.springframework.stereotype.Component;

/**
 * Estratégia de criação de cobrança via Cartão de Crédito.
 * Invoca o gateway de pagamento e preenche os campos específicos do cartão
 * na entidade {@link Cobranca}: {@code transactionId} (sempre), e
 * {@code acsUrl} e {@code threeDsPayload} somente quando retornados pelo gateway.
 */
@Component
public class CartaoCreditoCobrancaCriacaoStrategy implements CobrancaCriacaoStrategy {

    private final PagamentoGatewayClient pagamentoGatewayClient;

    public CartaoCreditoCobrancaCriacaoStrategy(PagamentoGatewayClient pagamentoGatewayClient) {
        this.pagamentoGatewayClient = pagamentoGatewayClient;
    }

    @Override
    public CobrancaMetodoEnum getMetodo() {
        return CobrancaMetodoEnum.CARTAO_CREDITO;
    }

    @Override
    public void executar(Cobranca cobranca, CobrancaRequestDTO request) {
        GatewayCartaoRequest gatewayRequest = new GatewayCartaoRequest(
                cobranca.getValorSolicitacao(),
                cobranca.getIdUsuario()
        );

        GatewayCartaoResponse response = pagamentoGatewayClient.iniciarCobrancaCartao(gatewayRequest);

        cobranca.setTransactionId(response.transactionId());

        if (response.acsUrl() != null) {
            cobranca.setAcsUrl(response.acsUrl());
        }

        if (response.threeDsPayload() != null) {
            cobranca.setThreeDsPayload(response.threeDsPayload());
        }
    }
}
