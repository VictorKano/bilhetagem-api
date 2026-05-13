package com.bilhetagem.cobranca.dto;

import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CobrancaRequestDTO(
        @NotNull @DecimalMin("0.01") BigDecimal valor,
        CobrancaTipoEnum tipo,
        CobrancaMetodoEnum metodo
) {}
