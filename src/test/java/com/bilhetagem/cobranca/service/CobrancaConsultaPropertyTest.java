package com.bilhetagem.cobranca.service;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import com.bilhetagem.cobranca.integration.CheckoutValidationClient;
import com.bilhetagem.cobranca.integration.ConsultaStatusClient;
import com.bilhetagem.cobranca.integration.UserContext;
import com.bilhetagem.cobranca.lock.LockExecutor;
import com.bilhetagem.cobranca.lock.LockService;
import com.bilhetagem.cobranca.repository.CobrancaRepository;
import com.bilhetagem.cobranca.service.strategy.CobrancaCriacaoStrategyRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CobrancaConsultaTest {

    private CobrancaService buildService(CobrancaRepository cobrancaRepository,
                                         ConsultaStatusClient consultaStatusClient) {
        LockService lockService = mock(LockService.class);
        when(lockService.tryAcquire(anyString(), anyLong())).thenReturn(true);
        LockExecutor lockExecutor = new LockExecutor(lockService);
        CobrancaCriacaoStrategyRegistry strategyRegistry = mock(CobrancaCriacaoStrategyRegistry.class);
        UserContext userContext = mock(UserContext.class);
        CheckoutValidationClient checkoutValidationClient = mock(CheckoutValidationClient.class);
        return new CobrancaService(lockExecutor, strategyRegistry, cobrancaRepository,
                userContext, consultaStatusClient, checkoutValidationClient, null);
    }

    private Cobranca buildCobranca(Long id, CobrancaMetodoEnum metodo, CobrancaStatusEnum status, String txid) {
        Cobranca cobranca = new Cobranca();
        cobranca.setId(id);
        cobranca.setIdUsuario("usuario-consulta");
        cobranca.setNomeSolicitante("Usuario Consulta");
        cobranca.setTipo(CobrancaTipoEnum.RECARGA);
        cobranca.setMetodo(metodo);
        cobranca.setStatus(status);
        cobranca.setValorSolicitacao(new BigDecimal("100.00"));
        cobranca.setTxid(txid);
        cobranca.setDataCriacao(LocalDateTime.now());
        return cobranca;
    }

    @ParameterizedTest
    @EnumSource(value = CobrancaStatusEnum.class, names = {"SOLICITADA", "EXPIRADA", "ERRO_APROVACAO_PEDIDO", "EM_REPROCESSAMENTO", "ERRO_ANALISE_PENDENTE"})
    void pixComStatusPendenteConsultaStatusClientEInvocado(CobrancaStatusEnum statusPendente) {
        String txid = "txid-pix-pendente";
        Cobranca cobrancaPix = buildCobranca(1L, CobrancaMetodoEnum.PIX, statusPendente, txid);

        CobrancaRepository cobrancaRepository = mock(CobrancaRepository.class);
        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobrancaPix));

        ConsultaStatusClient consultaStatusClient = mock(ConsultaStatusClient.class);
        when(consultaStatusClient.consultarStatus(txid)).thenReturn(statusPendente);

        buildService(cobrancaRepository, consultaStatusClient).consultarCobranca(1L);

        verify(consultaStatusClient).consultarStatus(txid);
    }

    @Test
    void pixComStatusFinalizadaConsultaStatusClientNaoEInvocado() {
        Cobranca cobrancaPixFinalizada = buildCobranca(2L, CobrancaMetodoEnum.PIX,
                CobrancaStatusEnum.FINALIZADA, "txid-finalizado");

        CobrancaRepository cobrancaRepository = mock(CobrancaRepository.class);
        when(cobrancaRepository.findById(2L)).thenReturn(Optional.of(cobrancaPixFinalizada));

        ConsultaStatusClient consultaStatusClient = mock(ConsultaStatusClient.class);
        buildService(cobrancaRepository, consultaStatusClient).consultarCobranca(2L);

        verify(consultaStatusClient, never()).consultarStatus(anyString());
    }

    @ParameterizedTest
    @EnumSource(CobrancaStatusEnum.class)
    void cartaoNaoInvocaConsultaStatusClient(CobrancaStatusEnum qualquerStatus) {
        Cobranca cobrancaCartao = buildCobranca(3L, CobrancaMetodoEnum.CARTAO_CREDITO, qualquerStatus, null);

        CobrancaRepository cobrancaRepository = mock(CobrancaRepository.class);
        when(cobrancaRepository.findById(3L)).thenReturn(Optional.of(cobrancaCartao));

        ConsultaStatusClient consultaStatusClient = mock(ConsultaStatusClient.class);
        buildService(cobrancaRepository, consultaStatusClient).consultarCobranca(3L);

        verify(consultaStatusClient, never()).consultarStatus(anyString());
    }
}
