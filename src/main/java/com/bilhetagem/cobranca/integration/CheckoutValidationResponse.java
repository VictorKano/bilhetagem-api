package com.bilhetagem.cobranca.integration;

public record CheckoutValidationResponse(
        String status,
        String authCode
) {}
