package com.bilhetagem.cobranca.service;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;
import com.bilhetagem.cobranca.dto.PixItemDTO;
import com.bilhetagem.cobranca.dto.PixWebhookDTO;
import com.bilhetagem.cobranca.integration.CheckoutValidationClient;
import com.bilhetagem.cobranca.integration.ConsultaStatusClient;
import com.bilhetagem.cobranca.integration.GatewayCartaoRequest;
import com.bilhetagem.cobranca.integration.GatewayCartaoResponse;
import com.bilhetagem.cobranca.integration.GatewayPixRequest;
import com.bilhetagem.cobranca.integration.GatewayPixResponse;
import com.bilhetagem.cobranca.integration.PagamentoGatewayClient;
import com.bilhetagem.cobranca.integration.UserContext;
import com.bilhetagem.cobranca.lock.LockExecutor;
import com.bilhetagem.cobranca.lock.LockService;
import com.bilhetagem.cobranca.repository.CobrancaRepository;
import com.bilhetagem.cobranca.service.strategy.CartaoCreditoCobrancaCriacaoStrategy;
import com.bilhetagem.cobranca.service.strategy.CobrancaCriacaoStrategyRegistry;
import com.bilhetagem.cobranca.service.strategy.PixCobrancaCriacaoStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CobrancaTimezoneTest {

    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    private static final PagamentoGatewayClient FAKE_GATEWAY = new PagamentoGatewayClient() {
        @Override
        public GatewayPixResponse iniciarCobrancaPix(GatewayPixRequest request) {
            return new GatewayPixResponse("txid-fake", "00020126...", ZonedDateTime.now().plusHours(1));
        }

        @Override
        public GatewayCartaoResponse iniciarCobrancaCartao(GatewayCartaoRequest request) {
            return new GatewayCartaoResponse("trans-fake", null, null);
        }
    };

    private CobrancaService buildService(CobrancaRepository cobrancaRepository) {
        LockService lockService = mock(LockService.class);
        when(lockService.tryAcquire(anyString(), anyLong())).thenReturn(true);
        LockExecutor lockExecutor = new LockExecutor(lockService);
        CobrancaCriacaoStrategyRegistry strategyRegistry = new CobrancaCriacaoStrategyRegistry(
                List.of(new PixCobrancaCriacaoStrategy(FAKE_GATEWAY),
                        new CartaoCreditoCobrancaCriacaoStrategy(FAKE_GATEWAY)));
        UserContext userContext = mock(UserContext.class);
        when(userContext.getIdUsuario()).thenReturn("usuario-teste");
        when(userContext.getGivenName()).thenReturn("João");
        when(userContext.getFamilyName()).thenReturn("Silva");
        when(userContext.getCpf()).thenReturn("000.000.000-00");
        return new CobrancaService(lockExecutor, strategyRegistry, cobrancaRepository,
                userContext, mock(ConsultaStatusClient.class), mock(CheckoutValidationClient.class), null);
    }

    @Test
    void criarCobrancaDataCriacaoNoFusoSaoPaulo() {
        CobrancaRepository repo = mock(CobrancaRepository.class);
        LocalDateTime antes = ZonedDateTime.now(ZONE_SP).toLocalDateTime().minusSeconds(2);

        buildService(repo).criarCobranca(
                new CobrancaRequestDTO(new BigDecimal("50.00"), CobrancaTipoEnum.RECARGA, CobrancaMetodoEnum.PIX));

        LocalDateTime depois = ZonedDateTime.now(ZONE_SP).toLocalDateTime().plusSeconds(2);
        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(repo).save(captor.capture());

        assertThat(captor.getValue().getDataCriacao())
                .isNotNull()
                .isAfterOrEqualTo(antes)
                .isBeforeOrEqualTo(depois);
    }

    @Test
    void processarWebhookPixDataFinalizadaNoFusoSaoPaulo() {
        CobrancaRepository repo = mock(CobrancaRepository.class);
        Cobranca cobranca = new Cobranca();
        cobranca.setId(1L);
        cobranca.setIdUsuario("usuario-teste");
        cobranca.setNomeSolicitante("João Silva");
        cobranca.setTipo(CobrancaTipoEnum.RECARGA);
        cobranca.setMetodo(CobrancaMetodoEnum.PIX);
        cobranca.setStatus(CobrancaStatusEnum.SOLICITADA);
        cobranca.setValorSolicitacao(new BigDecimal("50.00"));
        cobranca.setTxid("txid-tz");
        cobranca.setDataCriacao(LocalDateTime.now());
        when(repo.findTopByTxidOrderByIdDesc("txid-tz")).thenReturn(Optional.of(cobranca));

        LocalDateTime antes = ZonedDateTime.now(ZONE_SP).toLocalDateTime().minusSeconds(2);
        buildService(repo).processarWebhookPix(new PixWebhookDTO(
                List.of(new PixItemDTO("txid-tz", new BigDecimal("50.00"), "e2e-1"))));
        LocalDateTime depois = ZonedDateTime.now(ZONE_SP).toLocalDateTime().plusSeconds(2);

        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(repo).save(captor.capture());

        assertThat(captor.getValue().getDataFinalizada())
                .isNotNull()
                .isAfterOrEqualTo(antes)
                .isBeforeOrEqualTo(depois);
    }

    @Test
    void zoneIdSaoPauloReconhecidoPelaJvm() {
        ZoneId zoneSp = ZoneId.of("America/Sao_Paulo");
        assertThat(zoneSp).isNotNull();
        assertThat(ZonedDateTime.now(zoneSp).toLocalDateTime()).isNotNull();
    }
}
