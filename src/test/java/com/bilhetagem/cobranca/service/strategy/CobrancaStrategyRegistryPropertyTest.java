package com.bilhetagem.cobranca.service.strategy;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;
import com.bilhetagem.cobranca.exception.NegocioException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CobrancaStrategyRegistryTest {

    private CobrancaCriacaoStrategy mockStrategy(CobrancaMetodoEnum metodo) {
        return new CobrancaCriacaoStrategy() {
            @Override
            public CobrancaMetodoEnum getMetodo() {
                return metodo;
            }

            @Override
            public void executar(Cobranca cobranca, CobrancaRequestDTO request) {
            }
        };
    }

    @ParameterizedTest
    @EnumSource(CobrancaMetodoEnum.class)
    void getStrategyRetornaEstrategiaCorretaParaMetodoRegistrado(CobrancaMetodoEnum metodo) {
        CobrancaCriacaoStrategy pixStrategy = mockStrategy(CobrancaMetodoEnum.PIX);
        CobrancaCriacaoStrategy cartaoStrategy = mockStrategy(CobrancaMetodoEnum.CARTAO_CREDITO);

        CobrancaCriacaoStrategyRegistry registry =
                new CobrancaCriacaoStrategyRegistry(List.of(pixStrategy, cartaoStrategy));

        Map<CobrancaMetodoEnum, CobrancaCriacaoStrategy> expectedStrategies = Map.of(
                CobrancaMetodoEnum.PIX, pixStrategy,
                CobrancaMetodoEnum.CARTAO_CREDITO, cartaoStrategy
        );

        CobrancaCriacaoStrategy result = registry.getStrategy(metodo);

        assertThat(result)
                .as("getStrategy(%s) deve retornar a implementação correta", metodo)
                .isSameAs(expectedStrategies.get(metodo));
    }

    @ParameterizedTest
    @EnumSource(CobrancaMetodoEnum.class)
    void getStrategyLancaNegocioExceptionParaMetodoNaoRegistrado(CobrancaMetodoEnum metodo) {
        CobrancaCriacaoStrategyRegistry emptyRegistry =
                new CobrancaCriacaoStrategyRegistry(List.of());

        assertThatThrownBy(() -> emptyRegistry.getStrategy(metodo))
                .as("getStrategy(%s) em registry vazio deve lançar NegocioException", metodo)
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining(metodo.name());
    }
}
