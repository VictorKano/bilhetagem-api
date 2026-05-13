package com.bilhetagem.cobranca.service;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;
import com.bilhetagem.cobranca.integration.CheckoutValidationClient;
import com.bilhetagem.cobranca.integration.ConsultaStatusClient;
import com.bilhetagem.cobranca.integration.GatewayCartaoResponse;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CobrancaCriacaoTest {

    private CobrancaService buildService(CobrancaRepository cobrancaRepository,
                                         PagamentoGatewayClient gatewayClient) {
        LockService lockService = mock(LockService.class);
        when(lockService.tryAcquire(anyString(), anyLong())).thenReturn(true);
        LockExecutor lockExecutor = new LockExecutor(lockService);
        CobrancaCriacaoStrategyRegistry strategyRegistry = new CobrancaCriacaoStrategyRegistry(
                List.of(new PixCobrancaCriacaoStrategy(gatewayClient),
                        new CartaoCreditoCobrancaCriacaoStrategy(gatewayClient)));
        UserContext userContext = mock(UserContext.class);
        when(userContext.getIdUsuario()).thenReturn("usuario-teste");
        when(userContext.getGivenName()).thenReturn("João");
        when(userContext.getFamilyName()).thenReturn("Silva");
        when(userContext.getCpf()).thenReturn("000.000.000-00");
        return new CobrancaService(lockExecutor, strategyRegistry, cobrancaRepository,
                userContext, mock(ConsultaStatusClient.class), mock(CheckoutValidationClient.class), null);
    }

    @Test
    void cobrancaPixPersistidaComInvariantesCorretos() {
        CobrancaRepository repo = mock(CobrancaRepository.class);
        PagamentoGatewayClient gateway = mock(PagamentoGatewayClient.class);
        when(gateway.iniciarCobrancaPix(any())).thenReturn(
                new GatewayPixResponse("txid-gen", "copia-e-cola", ZonedDateTime.now().plusHours(1)));

        buildService(repo, gateway).criarCobranca(
                new CobrancaRequestDTO(new BigDecimal("50.00"), CobrancaTipoEnum.RECARGA, CobrancaMetodoEnum.PIX));

        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(repo).save(captor.capture());
        Cobranca persistida = captor.getValue();

        assertThat(persistida.getStatus()).isEqualTo(CobrancaStatusEnum.SOLICITADA);
        assertThat(persistida.getNomeSolicitante()).isNotNull().isNotBlank();
        assertThat(persistida.getDataCriacao()).isNotNull();
    }

    @Test
    void cobrancaPixCamposPreenchidosComRespostaDoGateway() {
        CobrancaRepository repo = mock(CobrancaRepository.class);
        PagamentoGatewayClient gateway = mock(PagamentoGatewayClient.class);
        ZonedDateTime dataExpiracao = ZonedDateTime.now().plusHours(1);
        when(gateway.iniciarCobrancaPix(any())).thenReturn(
                new GatewayPixResponse("txid-123", "copia-e-cola-123", dataExpiracao));

        buildService(repo, gateway).criarCobranca(
                new CobrancaRequestDTO(new BigDecimal("50.00"), CobrancaTipoEnum.RECARGA, CobrancaMetodoEnum.PIX));

        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(repo).save(captor.capture());
        Cobranca persistida = captor.getValue();

        assertThat(persistida.getTxid()).isEqualTo("txid-123");
        assertThat(persistida.getCopiaECola()).isEqualTo("copia-e-cola-123");
        assertThat(persistida.getDataExpiracao()).isEqualTo(
                dataExpiracao.withZoneSameInstant(ZoneId.of("America/Sao_Paulo")).toLocalDateTime());
    }

    @Test
    void cobrancaCartaoTransactionIdCorrespondeAoGateway() {
        CobrancaRepository repo = mock(CobrancaRepository.class);
        PagamentoGatewayClient gateway = mock(PagamentoGatewayClient.class);
        when(gateway.iniciarCobrancaCartao(any())).thenReturn(
                new GatewayCartaoResponse("trans-abc-123", null, null));

        buildService(repo, gateway).criarCobranca(
                new CobrancaRequestDTO(new BigDecimal("150.00"), CobrancaTipoEnum.RECARGA, CobrancaMetodoEnum.CARTAO_CREDITO));

        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(repo).save(captor.capture());

        assertThat(captor.getValue().getTransactionId()).isEqualTo("trans-abc-123");
    }
}
