package com.bilhetagem.cobranca.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.UUID;

@Component
@Profile({"test", "local"})
public class FakePagamentoGatewayClient implements PagamentoGatewayClient {

    @Override
    public GatewayPixResponse iniciarCobrancaPix(GatewayPixRequest request) {
        return new GatewayPixResponse(
                "txid-" + UUID.randomUUID(),
                "00020126...",
                ZonedDateTime.now().plusHours(1)
        );
    }

    @Override
    public GatewayCartaoResponse iniciarCobrancaCartao(GatewayCartaoRequest request) {
        return new GatewayCartaoResponse(
                "trans-" + UUID.randomUUID(),
                null,
                null
        );
    }
}
