package com.bilhetagem.cobranca.integration;

import java.time.ZonedDateTime;

public record GatewayPixResponse(
        String txid,
        String copiaECola,
        ZonedDateTime dataExpiracao
) {}
