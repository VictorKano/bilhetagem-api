package com.bilhetagem.cobranca.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CobrancaStatusEnumTest {

    private static final Map<CobrancaStatusEnum, Integer> EXPECTED_CODES = Map.of(
            CobrancaStatusEnum.SOLICITADA, 2,
            CobrancaStatusEnum.EXPIRADA, 3,
            CobrancaStatusEnum.ERRO_APROVACAO_PEDIDO, 4,
            CobrancaStatusEnum.FINALIZADA, 5,
            CobrancaStatusEnum.EM_REPROCESSAMENTO, 6,
            CobrancaStatusEnum.ERRO_ANALISE_PENDENTE, 9
    );

    @ParameterizedTest
    @EnumSource(CobrancaStatusEnum.class)
    void getCodigoRetornaCodigoCorreto(CobrancaStatusEnum status) {
        int expectedCodigo = EXPECTED_CODES.get(status);
        assertThat(status.getCodigo())
                .as("getCodigo() para %s deve retornar %d", status.name(), expectedCodigo)
                .isEqualTo(expectedCodigo);
    }
}
