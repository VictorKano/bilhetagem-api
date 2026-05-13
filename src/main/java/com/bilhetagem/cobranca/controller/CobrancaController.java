package com.bilhetagem.cobranca.controller;

import com.bilhetagem.cobranca.dto.CheckoutValidateRequestDTO;
import com.bilhetagem.cobranca.dto.CobrancaBasicoResponseDTO;
import com.bilhetagem.cobranca.dto.CobrancaCompletoResponseDTO;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;
import com.bilhetagem.cobranca.dto.PixWebhookDTO;
import com.bilhetagem.cobranca.service.CobrancaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para operações de cobrança.
 *
 * <p>Expõe os endpoints de criação, consulta, webhook PIX e validação de checkout
 * sob o path base {@code /api/v1/cobrancas}.
 */
@RestController
@RequestMapping("/api/v1/cobrancas")
public class CobrancaController {

    private final CobrancaService cobrancaService;

    public CobrancaController(CobrancaService cobrancaService) {
        this.cobrancaService = cobrancaService;
    }

    /**
     * Cria uma nova cobrança.
     *
     * @param request dados da requisição de criação
     * @return 201 Created com o DTO básico da cobrança criada
     */
    @PostMapping
    public ResponseEntity<CobrancaBasicoResponseDTO> criarCobranca(
            @Valid @RequestBody CobrancaRequestDTO request) {
        return ResponseEntity.status(201).body(cobrancaService.criarCobranca(request));
    }

    /**
     * Consulta uma cobrança pelo seu identificador.
     *
     * @param id identificador da cobrança
     * @return 200 OK com o DTO completo da versão mais recente da cobrança
     */
    @GetMapping("/{id}")
    public ResponseEntity<CobrancaCompletoResponseDTO> consultarCobranca(
            @PathVariable Long id) {
        return ResponseEntity.ok(cobrancaService.consultarCobranca(id));
    }

    /**
     * Processa notificações de pagamento PIX recebidas via webhook.
     *
     * @param dto payload do webhook PIX
     * @return 200 OK
     */
    @PostMapping("/webhook/pix")
    public ResponseEntity<Void> processarWebhookPix(@RequestBody PixWebhookDTO dto) {
        cobrancaService.processarWebhookPix(dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Valida o checkout de uma cobrança de cartão de crédito com autenticação 3DS.
     *
     * @param transactionId identificador da transação de cartão
     * @param request       dados de validação 3DS (cavv, xid, eci)
     * @return 200 OK
     */
    @PostMapping("/{transactionId}/validate")
    public ResponseEntity<Void> validarCheckout(
            @PathVariable String transactionId,
            @Valid @RequestBody CheckoutValidateRequestDTO request) {
        cobrancaService.validarCheckout(transactionId, request);
        return ResponseEntity.ok().build();
    }
}
