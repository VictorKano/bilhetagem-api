package com.bilhetagem.cobranca.service;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import com.bilhetagem.cobranca.dto.PixItemDTO;
import com.bilhetagem.cobranca.dto.PixWebhookDTO;
import com.bilhetagem.cobranca.integration.CheckoutValidationClient;
import com.bilhetagem.cobranca.integration.ConsultaStatusClient;
import com.bilhetagem.cobranca.integration.UserContext;
import com.bilhetagem.cobranca.lock.LockExecutor;
import com.bilhetagem.cobranca.lock.LockService;
import com.bilhetagem.cobranca.repository.CobrancaRepository;
import com.bilhetagem.cobranca.service.strategy.CobrancaCriacaoStrategyRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PixWebhookTest {

    private CobrancaService buildService(CobrancaRepository cobrancaRepository) {
        LockService lockService = mock(LockService.class);
        when(lockService.tryAcquire(anyString(), anyLong())).thenReturn(true);
        LockExecutor lockExecutor = new LockExecutor(lockService);
        CobrancaCriacaoStrategyRegistry strategyRegistry = mock(CobrancaCriacaoStrategyRegistry.class);
        UserContext userContext = mock(UserContext.class);
        ConsultaStatusClient consultaStatusClient = mock(ConsultaStatusClient.class);
        CheckoutValidationClient checkoutValidationClient = mock(CheckoutValidationClient.class);
        return new CobrancaService(lockExecutor, strategyRegistry, cobrancaRepository,
                userContext, consultaStatusClient, checkoutValidationClient, null);
    }

    private Cobranca buildCobranca(Long id, CobrancaStatusEnum status, String txid, BigDecimal valor) {
        Cobranca cobranca = new Cobranca();
        cobranca.setId(id);
        cobranca.setIdUsuario("usuario-teste");
        cobranca.setNomeSolicitante("Usuário Teste");
        cobranca.setTipo(CobrancaTipoEnum.RECARGA);
        cobranca.setMetodo(CobrancaMetodoEnum.PIX);
        cobranca.setStatus(status);
        cobranca.setValorSolicitacao(valor);
        cobranca.setTxid(txid);
        cobranca.setDataCriacao(LocalDateTime.now());
        return cobranca;
    }

    @Test
    void cobrancaFinalizadaSaveNaoEInvocado() {
        CobrancaRepository repo = mock(CobrancaRepository.class);
        Cobranca cobrancaFinalizada = buildCobranca(1L, CobrancaStatusEnum.FINALIZADA, "txid-123", new BigDecimal("50.00"));
        when(repo.findTopByTxidOrderByIdDesc("txid-123")).thenReturn(Optional.of(cobrancaFinalizada));

        buildService(repo).processarWebhookPix(new PixWebhookDTO(
                List.of(new PixItemDTO("txid-123", new BigDecimal("50.00"), "e2e-1"))));

        verify(repo, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = CobrancaStatusEnum.class, names = {"SOLICITADA", "EXPIRADA", "ERRO_APROVACAO_PEDIDO", "EM_REPROCESSAMENTO", "ERRO_ANALISE_PENDENTE"})
    void cobrancaNaoFinalizadaVersaoFilhaCriadaComCamposCorretos(CobrancaStatusEnum statusOriginal) {
        CobrancaRepository repo = mock(CobrancaRepository.class);
        BigDecimal valorItem = new BigDecimal("30.00");
        Cobranca cobrancaOriginal = buildCobranca(42L, statusOriginal, "txid-pend", new BigDecimal("31.00"));
        when(repo.findTopByTxidOrderByIdDesc("txid-pend")).thenReturn(Optional.of(cobrancaOriginal));

        buildService(repo).processarWebhookPix(new PixWebhookDTO(
                List.of(new PixItemDTO("txid-pend", valorItem, "e2e-1"))));

        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(repo, times(1)).save(captor.capture());

        Cobranca versaoFilha = captor.getValue();
        assertThat(versaoFilha.getStatus()).isEqualTo(CobrancaStatusEnum.FINALIZADA);
        assertThat(versaoFilha.getDataFinalizada()).isNotNull();
        assertThat(versaoFilha.getValorPago()).isEqualByComparingTo(valorItem);
        assertThat(versaoFilha.getVersaoPai()).isSameAs(cobrancaOriginal);
    }

    @Test
    void listaComTxidNuloEVazioApenasValidosSaoProcessados() {
        CobrancaRepository repo = mock(CobrancaRepository.class);
        Cobranca cobranca = buildCobranca(1L, CobrancaStatusEnum.SOLICITADA, "txid-valido", new BigDecimal("10.00"));
        when(repo.findTopByTxidOrderByIdDesc("txid-valido")).thenReturn(Optional.of(cobranca));

        List<PixItemDTO> itens = List.of(
                new PixItemDTO("txid-valido", new BigDecimal("10.00"), "e2e-1"),
                new PixItemDTO(null, new BigDecimal("5.00"), "e2e-null"),
                new PixItemDTO("", new BigDecimal("5.00"), "e2e-empty"),
                new PixItemDTO("   ", new BigDecimal("5.00"), "e2e-blank")
        );

        buildService(repo).processarWebhookPix(new PixWebhookDTO(itens));

        verify(repo, times(1)).save(any(Cobranca.class));
    }
}
