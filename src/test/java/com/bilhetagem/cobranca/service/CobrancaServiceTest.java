package com.bilhetagem.cobranca.service;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import com.bilhetagem.cobranca.dto.CobrancaBasicoResponseDTO;
import com.bilhetagem.cobranca.dto.CobrancaCompletoResponseDTO;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;
import com.bilhetagem.cobranca.dto.PixItemDTO;
import com.bilhetagem.cobranca.dto.PixWebhookDTO;
import com.bilhetagem.cobranca.exception.LockNotAcquiredException;
import com.bilhetagem.cobranca.exception.NegocioException;
import com.bilhetagem.cobranca.dto.CheckoutValidateRequestDTO;
import com.bilhetagem.cobranca.integration.CheckoutValidationClient;
import com.bilhetagem.cobranca.integration.CheckoutValidationResponse;
import com.bilhetagem.cobranca.integration.ConsultaStatusClient;
import com.bilhetagem.cobranca.integration.UserContext;
import com.bilhetagem.cobranca.lock.LockExecutor;
import com.bilhetagem.cobranca.lock.LockService;
import com.bilhetagem.cobranca.repository.CobrancaRepository;
import com.bilhetagem.cobranca.service.strategy.CobrancaCriacaoStrategy;
import com.bilhetagem.cobranca.service.strategy.CobrancaCriacaoStrategyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CobrancaServiceTest {

    @Mock
    private CobrancaRepository cobrancaRepository;

    @Mock
    private CobrancaCriacaoStrategyRegistry strategyRegistry;

    @Mock
    private UserContext userContext;

    @Mock
    private ConsultaStatusClient ConsultaStatusClient;

    @Mock
    private CheckoutValidationClient checkoutValidationClient;

    @Mock
    private CobrancaCriacaoStrategy pixStrategy;

    private LockExecutor lockExecutor;
    private CobrancaService cobrancaService;

    @BeforeEach
    void setUp() {
        LockService lockService = mock(LockService.class);
        lenient().when(lockService.tryAcquire(any(), anyLong())).thenReturn(true);
        lockExecutor = new LockExecutor(lockService);

        cobrancaService = new CobrancaService(
                lockExecutor,
                strategyRegistry,
                cobrancaRepository,
                userContext,
                ConsultaStatusClient,
                checkoutValidationClient,
                null
        );
    }

    @Test
    void criarCobrancaPixComSucessoDeveSalvarERetornarCamposEsperados() {
        when(userContext.getIdUsuario()).thenReturn("usuario-001");
        when(userContext.getGivenName()).thenReturn("Joao");
        when(userContext.getFamilyName()).thenReturn("Silva");
        when(strategyRegistry.getStrategy(CobrancaMetodoEnum.PIX)).thenReturn(pixStrategy);
        doAnswer(invocation -> {
            Cobranca cobranca = invocation.getArgument(0);
            cobranca.setTxid("txid-pix-001");
            cobranca.setCopiaECola("00020126580014br.gov.bcb.pix...");
            cobranca.setDataExpiracao(LocalDateTime.now().plusMinutes(30));
            return null;
        }).when(pixStrategy).executar(any(Cobranca.class), any(CobrancaRequestDTO.class));
        when(cobrancaRepository.save(any(Cobranca.class))).thenAnswer(invocation -> {
            Cobranca cobranca = invocation.getArgument(0);
            cobranca.setId(42L);
            return cobranca;
        });

        CobrancaRequestDTO request = new CobrancaRequestDTO(
                new BigDecimal("150.00"),
                CobrancaTipoEnum.RECARGA,
                CobrancaMetodoEnum.PIX
        );
        CobrancaBasicoResponseDTO response = cobrancaService.criarCobranca(request);
        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(cobrancaRepository, times(1)).save(captor.capture());

        Cobranca cobrancaSalva = captor.getValue();
        assertThat(cobrancaSalva.getStatus()).isEqualTo(CobrancaStatusEnum.SOLICITADA);
        assertThat(cobrancaSalva.getMetodo()).isEqualTo(CobrancaMetodoEnum.PIX);
        assertThat(cobrancaSalva.getNomeSolicitante()).isEqualTo("Joao Silva");
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.txid()).isEqualTo("txid-pix-001");
        assertThat(response.copiaECola()).isEqualTo("00020126580014br.gov.bcb.pix...");
        assertThat(response.dataExpiracao()).isNotNull();
    }

    @Test
    void criarCobrancaLockIndisponivelNaoDeveSalvarEDeveLancarLockNotAcquiredException() {
        LockExecutor lockExecutorMock = mock(LockExecutor.class);
        when(lockExecutorMock.execute(any(), anyLong(), any()))
                .thenThrow(new LockNotAcquiredException("Geracao de cobranca em andamento"));

        CobrancaService serviceComLockIndisponivel = new CobrancaService(
                lockExecutorMock,
                strategyRegistry,
                cobrancaRepository,
                userContext,
                ConsultaStatusClient,
                checkoutValidationClient,
                null
        );

        when(userContext.getIdUsuario()).thenReturn("usuario-002");
        when(userContext.getGivenName()).thenReturn("Maria");
        when(userContext.getFamilyName()).thenReturn("Souza");

        CobrancaRequestDTO request = new CobrancaRequestDTO(
                new BigDecimal("200.00"),
                CobrancaTipoEnum.RECARGA,
                CobrancaMetodoEnum.PIX
        );
        assertThatThrownBy(() -> serviceComLockIndisponivel.criarCobranca(request))
                .isInstanceOf(LockNotAcquiredException.class);
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    void criarCobrancaExcecaoInesperadaDeveLancarNegocioExceptionComMensagemCorreta() {
        when(userContext.getIdUsuario()).thenReturn("usuario-003");
        when(userContext.getGivenName()).thenReturn("Carlos");
        when(userContext.getFamilyName()).thenReturn("Pereira");
        when(strategyRegistry.getStrategy(CobrancaMetodoEnum.PIX)).thenReturn(pixStrategy);
        doThrow(new RuntimeException("Falha inesperada no gateway"))
                .when(pixStrategy).executar(any(Cobranca.class), any(CobrancaRequestDTO.class));

        CobrancaRequestDTO request = new CobrancaRequestDTO(
                new BigDecimal("75.00"),
                CobrancaTipoEnum.RECARGA,
                CobrancaMetodoEnum.PIX
        );
        assertThatThrownBy(() -> cobrancaService.criarCobranca(request))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Erro ao criar cobranca");
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    void processarWebhookPixCobrancaPendenteDeveFinalizarComVersaoFilha() {
        Cobranca cobrancaPendente = criarCobranca(1L, CobrancaStatusEnum.SOLICITADA, "txid-123");

        when(cobrancaRepository.findTopByTxidOrderByIdDesc("txid-123"))
                .thenReturn(Optional.of(cobrancaPendente));

        PixItemDTO item = new PixItemDTO("txid-123", new BigDecimal("50.00"), "e2e-001");
        PixWebhookDTO dto = new PixWebhookDTO(List.of(item));
        cobrancaService.processarWebhookPix(dto);
        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(cobrancaRepository, times(1)).save(captor.capture());

        Cobranca versaoFilhaSalva = captor.getValue();
        assertThat(versaoFilhaSalva.getStatus()).isEqualTo(CobrancaStatusEnum.FINALIZADA);
        assertThat(versaoFilhaSalva.getVersaoPai()).isSameAs(cobrancaPendente);
        assertThat(versaoFilhaSalva.getValorPago()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(versaoFilhaSalva.getDataFinalizada()).isNotNull();
        assertThat(versaoFilhaSalva.getTxid()).isEqualTo("txid-123");
    }

    @Test
    void processarWebhookPixCobrancaJaFinalizadaNaoDeveChamarSave() {
        Cobranca cobrancaFinalizada = criarCobranca(2L, CobrancaStatusEnum.FINALIZADA, "txid-456");

        when(cobrancaRepository.findTopByTxidOrderByIdDesc("txid-456"))
                .thenReturn(Optional.of(cobrancaFinalizada));

        PixItemDTO item = new PixItemDTO("txid-456", new BigDecimal("100.00"), "e2e-002");
        PixWebhookDTO dto = new PixWebhookDTO(List.of(item));
        cobrancaService.processarWebhookPix(dto);
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    void consultarCobrancaComVersaoFilhaDeveRetornarVersaoTerminal() {
        Cobranca cobranca3 = criarCobranca(3L, CobrancaStatusEnum.FINALIZADA, "txid-chain");
        cobranca3.setVersaoFilha(null);

        Cobranca cobranca2 = criarCobranca(2L, CobrancaStatusEnum.SOLICITADA, "txid-chain");
        cobranca2.setVersaoFilha(cobranca3);

        Cobranca cobranca1 = criarCobranca(1L, CobrancaStatusEnum.SOLICITADA, "txid-chain");
        cobranca1.setVersaoFilha(cobranca2);

        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca1));
        CobrancaCompletoResponseDTO resultado = cobrancaService.consultarCobranca(1L);
        assertThat(resultado.id()).isEqualTo(3L);
        assertThat(resultado.status()).isEqualTo(CobrancaStatusEnum.FINALIZADA);
        verify(ConsultaStatusClient, never()).consultarStatus(any());
    }

    @Test
    void consultarCobrancaStatusConsultaExternaIndisponivelDeveRetornarDadosAtuaisSemErro() {
        Cobranca cobrancaPix = criarCobranca(10L, CobrancaStatusEnum.SOLICITADA, "txid-pix-indisponivel");
        cobrancaPix.setMetodo(CobrancaMetodoEnum.PIX);
        cobrancaPix.setVersaoFilha(null);

        when(cobrancaRepository.findById(10L)).thenReturn(Optional.of(cobrancaPix));
        when(ConsultaStatusClient.consultarStatus("txid-pix-indisponivel"))
                .thenThrow(new RuntimeException("Servico externo indisponivel"));
        CobrancaCompletoResponseDTO resultado = cobrancaService.consultarCobranca(10L);
        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(10L);
        assertThat(resultado.status()).isEqualTo(CobrancaStatusEnum.SOLICITADA);
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    void validarCheckoutCobrancaExistenteDeveInvocarClienteEPersistirDadosAtualizados() {
        String transactionId = "txn-abc-123";
        Cobranca cobrancaExistente = criarCobrancaCartao(5L, CobrancaStatusEnum.SOLICITADA, transactionId);

        when(cobrancaRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.of(cobrancaExistente));

        CheckoutValidationResponse validationResponse =
                new CheckoutValidationResponse("FINALIZADA", "AUTH-CODE-XYZ");
        when(checkoutValidationClient.validar(eq(transactionId), any(CheckoutValidateRequestDTO.class)))
                .thenReturn(validationResponse);

        CheckoutValidateRequestDTO request =
                new CheckoutValidateRequestDTO("cavv-value", "xid-value", "05");
        cobrancaService.validarCheckout(transactionId, request);
        verify(checkoutValidationClient, times(1)).validar(eq(transactionId), eq(request));
        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(cobrancaRepository, times(1)).save(captor.capture());

        Cobranca cobrancaSalva = captor.getValue();
        assertThat(cobrancaSalva.getTransactionId()).isEqualTo(transactionId);
        assertThat(cobrancaSalva.getStatus()).isEqualTo(CobrancaStatusEnum.FINALIZADA);
    }

    private Cobranca criarCobranca(Long id, CobrancaStatusEnum status, String txid) {
        Cobranca cobranca = new Cobranca();
        cobranca.setId(id);
        cobranca.setIdUsuario("usuario-teste");
        cobranca.setNomeSolicitante("Usuario Teste");
        cobranca.setTipo(CobrancaTipoEnum.RECARGA);
        cobranca.setMetodo(CobrancaMetodoEnum.PIX);
        cobranca.setStatus(status);
        cobranca.setValorSolicitacao(new BigDecimal("100.00"));
        cobranca.setTxid(txid);
        cobranca.setDataCriacao(LocalDateTime.now());
        return cobranca;
    }

    private Cobranca criarCobrancaCartao(Long id, CobrancaStatusEnum status, String transactionId) {
        Cobranca cobranca = new Cobranca();
        cobranca.setId(id);
        cobranca.setIdUsuario("usuario-teste");
        cobranca.setNomeSolicitante("Usuario Teste");
        cobranca.setTipo(CobrancaTipoEnum.RECARGA);
        cobranca.setMetodo(CobrancaMetodoEnum.CARTAO_CREDITO);
        cobranca.setStatus(status);
        cobranca.setValorSolicitacao(new BigDecimal("200.00"));
        cobranca.setTransactionId(transactionId);
        cobranca.setDataCriacao(LocalDateTime.now());
        return cobranca;
    }
}
