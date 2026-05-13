package com.bilhetagem.cobranca.integration;

import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "local"})
public class FakeConsultaStatusClient implements ConsultaStatusClient {

    @Override
    public CobrancaStatusEnum consultarStatus(String txid) {
        return CobrancaStatusEnum.SOLICITADA;
    }
}
