package com.bilhetagem.cobranca.integration;

import java.math.BigDecimal;

public record GatewayCartaoRequest(
        BigDecimal valor,
        String idUsuario
) {}
