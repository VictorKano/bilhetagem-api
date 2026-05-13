package com.bilhetagem.cobranca.integration;

import com.bilhetagem.cobranca.dto.CheckoutValidateRequestDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "local"})
public class FakeCheckoutValidationClient implements CheckoutValidationClient {

    @Override
    public CheckoutValidationResponse validar(String transactionId, CheckoutValidateRequestDTO request) {
        return new CheckoutValidationResponse("APPROVED", "AUTH-FAKE-001");
    }
}
