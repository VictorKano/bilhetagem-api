package com.bilhetagem.cobranca.service.strategy;

import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.exception.NegocioException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry de estratégias de criação de cobrança.
 * Recebe todas as implementações de {@link CobrancaCriacaoStrategy} via injeção
 * de dependência do Spring e as indexa por {@link CobrancaMetodoEnum}.
 */
@Component
public class CobrancaCriacaoStrategyRegistry {

    private final Map<CobrancaMetodoEnum, CobrancaCriacaoStrategy> strategies;

    public CobrancaCriacaoStrategyRegistry(List<CobrancaCriacaoStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(CobrancaCriacaoStrategy::getMetodo, Function.identity()));
    }

    /**
     * Retorna a estratégia correspondente ao método de pagamento informado.
     *
     * @param metodo método de pagamento desejado
     * @return a implementação de {@link CobrancaCriacaoStrategy} registrada
     * @throws NegocioException se nenhuma estratégia estiver registrada para o método
     */
    public CobrancaCriacaoStrategy getStrategy(CobrancaMetodoEnum metodo) {
        return Optional.ofNullable(strategies.get(metodo))
                .orElseThrow(() -> new NegocioException("Método de pagamento não suportado: " + metodo));
    }
}
