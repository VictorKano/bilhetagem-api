package com.bilhetagem.cobranca.dto;

import java.math.BigDecimal;

public record PixItemDTO(
        String txid,
        BigDecimal valor,
        String endToEndId
) {}
