package com.bilhetagem.cobranca.service;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import com.bilhetagem.cobranca.dto.CobrancaCompletoResponseDTO;
import com.bilhetagem.cobranca.integration.CheckoutValidationClient;
import com.bilhetagem.cobranca.integration.ConsultaStatusClient;
import com.bilhetagem.cobranca.integration.UserContext;
import com.bilhetagem.cobranca.lock.LockExecutor;
import com.bilhetagem.cobranca.lock.LockService;
import com.bilhetagem.cobranca.repository.CobrancaRepository;
import com.bilhetagem.cobranca.service.strategy.CobrancaCriacaoStrategyRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CobrancaVersaoTest {

    private CobrancaService buildService(CobrancaRepository cobrancaRepository,
                                         CobrancaStatusEnum terminalStatus) {
        LockService lockService = mock(LockService.class);
        when(lockService.tryAcquire(anyString(), anyLong())).thenReturn(true);
        LockExecutor lockExecutor = new LockExecutor(lockService);
        CobrancaCriacaoStrategyRegistry strategyRegistry = mock(CobrancaCriacaoStrategyRegistry.class);
        UserContext userContext = mock(UserContext.class);
        CheckoutValidationClient checkoutValidationClient = mock(CheckoutValidationClient.class);
        ConsultaStatusClient consultaStatusClient = mock(ConsultaStatusClient.class);
        when(consultaStatusClient.consultarStatus(anyString())).thenReturn(terminalStatus);
        return new CobrancaService(lockExecutor, strategyRegistry, cobrancaRepository,
                userContext, consultaStatusClient, checkoutValidationClient, null);
    }

    private List<Cobranca> buildVersionChain(int chainLength) {
        List<Cobranca> chain = new ArrayList<>();
        for (int i = 0; i < chainLength; i++) {
            Cobranca cobranca = new Cobranca();
            cobranca.setId((long) (i + 1));
            cobranca.setIdUsuario("usuario-chain");
            cobranca.setNomeSolicitante("Usuario Chain");
            cobranca.setTipo(CobrancaTipoEnum.RECARGA);
            cobranca.setMetodo(CobrancaMetodoEnum.CARTAO_CREDITO);
            cobranca.setValorSolicitacao(new BigDecimal("100.00"));
            cobranca.setDataCriacao(LocalDateTime.now());
            cobranca.setTransactionId("txn-chain-" + i);
            cobranca.setStatus(i == chainLength - 1 ? CobrancaStatusEnum.FINALIZADA : CobrancaStatusEnum.SOLICITADA);
            chain.add(cobranca);
        }
        for (int i = 0; i < chainLength - 1; i++) {
            chain.get(i).setVersaoFilha(chain.get(i + 1));
        }
        return chain;
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 8, 10})
    void cadeiaDeVersoesConsultaRetornaVersaoTerminal(int chainLength) {
        List<Cobranca> chain = buildVersionChain(chainLength);
        Cobranca root = chain.get(0);
        Cobranca terminal = chain.get(chainLength - 1);

        CobrancaRepository cobrancaRepository = mock(CobrancaRepository.class);
        when(cobrancaRepository.findById(root.getId())).thenReturn(Optional.of(root));

        CobrancaCompletoResponseDTO resultado = buildService(cobrancaRepository, CobrancaStatusEnum.FINALIZADA)
                .consultarCobranca(root.getId());

        assertThat(resultado.id()).isEqualTo(terminal.getId());
        assertThat(resultado.status()).isEqualTo(terminal.getStatus());
    }
}
