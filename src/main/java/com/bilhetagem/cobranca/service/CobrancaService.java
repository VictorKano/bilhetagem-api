package com.bilhetagem.cobranca.service;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import com.bilhetagem.cobranca.dto.CheckoutValidateRequestDTO;
import com.bilhetagem.cobranca.dto.CobrancaBasicoResponseDTO;
import com.bilhetagem.cobranca.dto.CobrancaCompletoResponseDTO;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;
import com.bilhetagem.cobranca.dto.PixItemDTO;
import com.bilhetagem.cobranca.dto.PixWebhookDTO;
import com.bilhetagem.cobranca.exception.NegocioException;
import com.bilhetagem.cobranca.exception.RecursoNaoEncontradoException;
import com.bilhetagem.cobranca.integration.CheckoutValidationClient;
import com.bilhetagem.cobranca.integration.CheckoutValidationResponse;
import com.bilhetagem.cobranca.integration.ConsultaStatusClient;
import com.bilhetagem.cobranca.integration.UserContext;
import com.bilhetagem.cobranca.lock.LockExecutor;
import com.bilhetagem.cobranca.repository.CobrancaRepository;
import com.bilhetagem.cobranca.service.strategy.CobrancaCriacaoStrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Servico principal de orquestracao de cobrancas.
 *
 * <p>Responsavel por criar cobrancas com lock distribuido, delegar ao gateway via
 * strategy pattern, persistir a entidade e publicar eventos de dominio no Kafka.
 */
@Service
public class CobrancaService {

    private static final Logger log = LoggerFactory.getLogger(CobrancaService.class);

    private static final String TIMEZONE_SP = "America/Sao_Paulo";
    private static final String TOPIC_COBRANCA_CRIADA = "cobranca.criada";
    private static final String TOPIC_COBRANCA_FINALIZADA = "cobranca.finalizada";

    private static final Set<CobrancaStatusEnum> STATUS_PENDENTES = EnumSet.of(
            CobrancaStatusEnum.SOLICITADA,
            CobrancaStatusEnum.EXPIRADA,
            CobrancaStatusEnum.ERRO_APROVACAO_PEDIDO,
            CobrancaStatusEnum.EM_REPROCESSAMENTO,
            CobrancaStatusEnum.ERRO_ANALISE_PENDENTE
    );

    private final LockExecutor lockExecutor;
    private final CobrancaCriacaoStrategyRegistry strategyRegistry;
    private final CobrancaRepository cobrancaRepository;
    private final UserContext userContext;
    private final ConsultaStatusClient ConsultaStatusClient;
    private final CheckoutValidationClient checkoutValidationClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${cobranca.lock.ttl-seconds:5}")
    private long lockTtlSeconds;

    public CobrancaService(
            LockExecutor lockExecutor,
            CobrancaCriacaoStrategyRegistry strategyRegistry,
            CobrancaRepository cobrancaRepository,
            UserContext userContext,
            ConsultaStatusClient ConsultaStatusClient,
            CheckoutValidationClient checkoutValidationClient,
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.lockExecutor = lockExecutor;
        this.strategyRegistry = strategyRegistry;
        this.cobrancaRepository = cobrancaRepository;
        this.userContext = userContext;
        this.ConsultaStatusClient = ConsultaStatusClient;
        this.checkoutValidationClient = checkoutValidationClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Cria uma nova cobranca para o usuario autenticado.
     *
     * <p>O fluxo completo e executado dentro de um lock distribuido na chave
     * {@code cobrancas:{idUsuario}} para evitar duplicidade por requisicoes concorrentes.
     *
     * @param request dados da requisicao de criacao
     * @return DTO basico com os dados da cobranca criada
     * @throws NegocioException se ocorrer erro no gateway ou excecao inesperada
     */
    public CobrancaBasicoResponseDTO criarCobranca(CobrancaRequestDTO request) {
        String idUsuario = userContext.getIdUsuario();
        String givenName = userContext.getGivenName();
        String familyName = userContext.getFamilyName();

        CobrancaTipoEnum tipo = request.tipo() != null ? request.tipo() : CobrancaTipoEnum.RECARGA;
        CobrancaMetodoEnum metodo = request.metodo() != null ? request.metodo() : CobrancaMetodoEnum.PIX;

        String lockKey = "cobrancas:" + idUsuario;

        return lockExecutor.execute(lockKey, lockTtlSeconds, () -> {
            Cobranca cobranca = new Cobranca();
            cobranca.setIdUsuario(idUsuario);
            cobranca.setNomeSolicitante(givenName + " " + familyName);
            cobranca.setTipo(tipo);
            cobranca.setMetodo(metodo);
            cobranca.setStatus(CobrancaStatusEnum.SOLICITADA);
            cobranca.setValorSolicitacao(request.valor());
            cobranca.setDataCriacao(
                    ZonedDateTime.now(ZoneId.of(TIMEZONE_SP)).toLocalDateTime());

            try {
                strategyRegistry.getStrategy(metodo).executar(cobranca, request);
            } catch (Exception e) {
                log.error("Erro ao executar strategy de criacao de cobranca para metodo {}: {}",
                        metodo, e.getMessage(), e);
                throw new NegocioException("Erro ao criar cobranca");
            }

            try {
                cobrancaRepository.save(cobranca);
            } catch (Exception e) {
                log.error("Erro inesperado ao persistir cobranca para usuario {}: {}",
                        idUsuario, e.getMessage(), e);
                throw new NegocioException("Erro ao criar cobranca");
            }

            if (kafkaTemplate != null) {
                kafkaTemplate.send(TOPIC_COBRANCA_CRIADA, idUsuario, cobranca);
            }

            return new CobrancaBasicoResponseDTO(
                    cobranca.getId(),
                    cobranca.getTxid(),
                    cobranca.getCopiaECola(),
                    cobranca.getDataExpiracao(),
                    cobranca.getTransactionId());
        });
    }

    /**
     * Consulta uma cobranca pelo seu identificador, retornando a versao mais recente.
     *
     * <p>Percorre a cadeia de {@code versaoFilha} ate a versao terminal. Se a cobranca
     * for PIX e estiver em status pendente, consulta o status atualizado via
     * {@link ConsultaStatusClient}. Se o status mudou, persiste uma nova
     * {@code VersaoFilha} com o status atualizado.
     *
     * @param id identificador da cobranca
     * @return DTO completo com os dados da versao mais recente
     * @throws RecursoNaoEncontradoException se a cobranca nao for encontrada
     */
    public CobrancaCompletoResponseDTO consultarCobranca(Long id) {
        Cobranca cobranca = cobrancaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobranca nao encontrada: " + id));

        // Percorre a cadeia de versoes ate a versao terminal
        while (cobranca.getVersaoFilha() != null) {
            cobranca = cobranca.getVersaoFilha();
        }

        // Consulta status externo se PIX e status pendente
        if (cobranca.getMetodo() == CobrancaMetodoEnum.PIX && isPendente(cobranca.getStatus())) {
            try {
                CobrancaStatusEnum novoStatus = ConsultaStatusClient.consultarStatus(cobranca.getTxid());

                if (novoStatus != cobranca.getStatus()) {
                    Cobranca versaoFilha = new Cobranca();
                    versaoFilha.setIdUsuario(cobranca.getIdUsuario());
                    versaoFilha.setNomeSolicitante(cobranca.getNomeSolicitante());
                    versaoFilha.setTipo(cobranca.getTipo());
                    versaoFilha.setMetodo(cobranca.getMetodo());
                    versaoFilha.setStatus(novoStatus);
                    versaoFilha.setValorSolicitacao(cobranca.getValorSolicitacao());
                    versaoFilha.setValorPago(cobranca.getValorPago());
                    versaoFilha.setTxid(cobranca.getTxid());
                    versaoFilha.setCopiaECola(cobranca.getCopiaECola());
                    versaoFilha.setTransactionId(cobranca.getTransactionId());
                    versaoFilha.setAcsUrl(cobranca.getAcsUrl());
                    versaoFilha.setThreeDsPayload(cobranca.getThreeDsPayload());
                    versaoFilha.setDataCriacao(cobranca.getDataCriacao());
                    versaoFilha.setDataExpiracao(cobranca.getDataExpiracao());
                    versaoFilha.setDataFinalizada(cobranca.getDataFinalizada());
                    versaoFilha.setVersaoPai(cobranca);

                    cobrancaRepository.save(versaoFilha);
                    cobranca = versaoFilha;
                }
            } catch (Exception e) {
                log.warn("Falha ao consultar status externo para txid {}: {}", cobranca.getTxid(), e.getMessage());
            }
        }

        return new CobrancaCompletoResponseDTO(
                cobranca.getId(),
                cobranca.getTxid(),
                cobranca.getIdUsuario(),
                cobranca.getTipo(),
                cobranca.getMetodo(),
                cobranca.getStatus(),
                cobranca.getValorSolicitacao(),
                cobranca.getValorPago(),
                cobranca.getDataCriacao(),
                cobranca.getDataExpiracao(),
                cobranca.getDataFinalizada());
    }

    /**
     * Processa notificacoes de pagamento PIX recebidas via webhook.
     *
     * <p>Para cada item da lista, busca a cobranca mais recente pelo {@code txid} e,
     * se nao estiver finalizada, cria uma {@code VersaoFilha} com {@code status=FINALIZADA}.
     * Falhas individuais por item sao capturadas e registradas em log, sem interromper
     * o processamento dos demais itens.
     *
     * @param dto payload do webhook PIX
     */
    public void processarWebhookPix(PixWebhookDTO dto) {
        // Req 4.1: ignorar payload nulo ou lista vazia
        if (dto == null || dto.pix() == null || dto.pix().isEmpty()) {
            return;
        }

        for (PixItemDTO item : dto.pix()) {
            try {
                // Req 4.2: ignorar txid nulo ou vazio
                if (item.txid() == null || item.txid().isBlank()) {
                    continue;
                }

                // Req 4.3: buscar cobranca mais recente por txid
                Optional<Cobranca> optCobranca =
                        cobrancaRepository.findTopByTxidOrderByIdDesc(item.txid());

                // Req 4.4: ignorar se nao encontrada
                if (optCobranca.isEmpty()) {
                    continue;
                }

                Cobranca cobranca = optCobranca.get();

                // Req 4.5: ignorar se ja FINALIZADA
                if (CobrancaStatusEnum.FINALIZADA.equals(cobranca.getStatus())) {
                    continue;
                }

                // Req 4.6: criar VersaoFilha com status=FINALIZADA
                Cobranca versaoFilha = new Cobranca();
                versaoFilha.setIdUsuario(cobranca.getIdUsuario());
                versaoFilha.setNomeSolicitante(cobranca.getNomeSolicitante());
                versaoFilha.setTipo(cobranca.getTipo());
                versaoFilha.setMetodo(cobranca.getMetodo());
                versaoFilha.setStatus(CobrancaStatusEnum.FINALIZADA);
                versaoFilha.setValorSolicitacao(cobranca.getValorSolicitacao());
                versaoFilha.setValorPago(item.valor());
                versaoFilha.setTxid(cobranca.getTxid());
                versaoFilha.setCopiaECola(cobranca.getCopiaECola());
                versaoFilha.setTransactionId(cobranca.getTransactionId());
                versaoFilha.setAcsUrl(cobranca.getAcsUrl());
                versaoFilha.setThreeDsPayload(cobranca.getThreeDsPayload());
                versaoFilha.setDataCriacao(cobranca.getDataCriacao());
                versaoFilha.setDataExpiracao(cobranca.getDataExpiracao());
                // Req 7.2: dataFinalizada no fuso America/Sao_Paulo
                versaoFilha.setDataFinalizada(
                        ZonedDateTime.now(ZoneId.of(TIMEZONE_SP)).toLocalDateTime());
                versaoFilha.setVersaoPai(cobranca);

                cobrancaRepository.save(versaoFilha);

                // Req 8.7: publicar evento cobranca.finalizada no Kafka (se disponivel)
                if (kafkaTemplate != null) {
                    kafkaTemplate.send(TOPIC_COBRANCA_FINALIZADA, item.txid(), versaoFilha);
                }

            } catch (Exception e) {
                // Req 4.7: capturar excecao por item, registrar ERROR e continuar
                log.error("Erro ao processar item do webhook PIX para txid {}: {}",
                        item.txid(), e.getMessage(), e);
            }
        }
    }

    /**
     * Valida o checkout de uma cobranca de cartao de credito com autenticacao 3DS.
     *
     * <p>Busca a cobranca pelo {@code transactionId}, invoca o {@link CheckoutValidationClient}
     * com os dados fornecidos, atualiza a cobranca com os dados retornados e persiste.
     *
     * @param transactionId identificador da transacao de cartao
     * @param request       dados de validacao 3DS (cavv, xid, eci)
     * @throws RecursoNaoEncontradoException se nenhuma cobranca for encontrada para o transactionId
     * @throws NegocioException              se o CheckoutValidationClient lancar excecao
     */
    public void validarCheckout(String transactionId, CheckoutValidateRequestDTO request) {
        // Req 5.1: buscar cobranca por transactionId
        Cobranca cobranca = cobrancaRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cobranca nao encontrada para transactionId: " + transactionId));

        // Req 5.4: invocar CheckoutValidationClient
        CheckoutValidationResponse response;
        try {
            response = checkoutValidationClient.validar(transactionId, request);
        } catch (Exception e) {
            // Req 5.6: capturar excecao com log ERROR e lancar NegocioException
            log.error("Erro ao validar checkout para transactionId {}: {}", transactionId, e.getMessage(), e);
            throw new NegocioException("Erro ao validar checkout para transactionId: " + transactionId);
        }

        // Req 5.5: atualizar cobranca com dados retornados pelo cliente de validacao
        if (response.status() != null) {
            try {
                CobrancaStatusEnum novoStatus = CobrancaStatusEnum.valueOf(response.status());
                cobranca.setStatus(novoStatus);
            } catch (IllegalArgumentException e) {
                log.warn("Status retornado pelo CheckoutValidationClient nao mapeavel para CobrancaStatusEnum: {}",
                        response.status());
            }
        }

        // Req 5.5: persistir a entidade atualizada
        cobrancaRepository.save(cobranca);
    }

    /**
     * Verifica se o status da cobranca e considerado pendente para fins de consulta externa.
     *
     * @param status status atual da cobranca
     * @return {@code true} se o status for pendente
     */
    private boolean isPendente(CobrancaStatusEnum status) {
        return STATUS_PENDENTES.contains(status);
    }
}
