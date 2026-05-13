package com.bilhetagem.cobranca.integration;

import java.math.BigDecimal;

public record GatewayPixRequest(
        BigDecimal valor,
        String idUsuario
) {}
