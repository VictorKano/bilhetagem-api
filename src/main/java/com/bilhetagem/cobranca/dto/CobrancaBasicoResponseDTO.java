package com.bilhetagem.cobranca.dto;

import java.time.LocalDateTime;

/**
 * DTO de resposta básico retornado na criação de uma cobrança.
 * Contém os campos essenciais para o cliente iniciar o fluxo de pagamento.
 */
public record CobrancaBasicoResponseDTO(
        Long id,
        String txid,
        String copiaECola,
        LocalDateTime dataExpiracao,
        String transactionId
) {
}
