package com.bilhetagem.cobranca.integration;

public record GatewayCartaoResponse(
        String transactionId,
        String acsUrl,
        String threeDsPayload
) {}
