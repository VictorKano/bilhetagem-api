package com.bilhetagem.cobranca.integration;

public interface PagamentoGatewayClient {

    GatewayPixResponse iniciarCobrancaPix(GatewayPixRequest request);

    GatewayCartaoResponse iniciarCobrancaCartao(GatewayCartaoRequest request);
}
