package com.bilhetagem.cobranca.integration;

import com.bilhetagem.cobranca.dto.CheckoutValidateRequestDTO;

public interface CheckoutValidationClient {

    CheckoutValidationResponse validar(String transactionId, CheckoutValidateRequestDTO request);
}
