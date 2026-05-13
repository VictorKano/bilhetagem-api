package com.bilhetagem.cobranca.integration;

import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;

public interface ConsultaStatusClient {

    CobrancaStatusEnum consultarStatus(String txid);
}
