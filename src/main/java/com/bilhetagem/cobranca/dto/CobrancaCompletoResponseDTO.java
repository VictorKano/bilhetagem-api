package com.bilhetagem.cobranca.dto;

import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta completo retornado na consulta de uma cobrança.
 * Contém todos os campos relevantes para exibição do estado atual da cobrança.
 */
public record CobrancaCompletoResponseDTO(
        Long id,
        String txid,
        String idUsuario,
        CobrancaTipoEnum tipo,
        CobrancaMetodoEnum metodo,
        CobrancaStatusEnum status,
        BigDecimal valorSolicitado,
        BigDecimal valorPago,
        LocalDateTime dataCriacao,
        LocalDateTime dataExpiracao,
        LocalDateTime dataFinalizada
) {
}
