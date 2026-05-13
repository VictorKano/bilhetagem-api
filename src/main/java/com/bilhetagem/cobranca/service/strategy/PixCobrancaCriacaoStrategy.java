package com.bilhetagem.cobranca.service.strategy;

import com.bilhetagem.cobranca.domain.Cobranca;
import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;
import com.bilhetagem.cobranca.integration.GatewayPixRequest;
import com.bilhetagem.cobranca.integration.GatewayPixResponse;
import com.bilhetagem.cobranca.integration.PagamentoGatewayClient;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Estratégia de criação de cobrança via PIX.
 * Invoca o gateway de pagamento e preenche os campos específicos do PIX
 * na entidade {@link Cobranca}: {@code txid}, {@code copiaECola} e
 * {@code dataExpiracao} (convertida para o fuso {@code America/Sao_Paulo}).
 */
@Component
public class PixCobrancaCriacaoStrategy implements CobrancaCriacaoStrategy {

    private static final ZoneId ZONE_SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final PagamentoGatewayClient pagamentoGatewayClient;

    public PixCobrancaCriacaoStrategy(PagamentoGatewayClient pagamentoGatewayClient) {
        this.pagamentoGatewayClient = pagamentoGatewayClient;
    }

    @Override
    public CobrancaMetodoEnum getMetodo() {
        return CobrancaMetodoEnum.PIX;
    }

    @Override
    public void executar(Cobranca cobranca, CobrancaRequestDTO request) {
        GatewayPixRequest gatewayRequest = new GatewayPixRequest(
                cobranca.getValorSolicitacao(),
                cobranca.getIdUsuario()
        );

        GatewayPixResponse response = pagamentoGatewayClient.iniciarCobrancaPix(gatewayRequest);

        cobranca.setTxid(response.txid());
        cobranca.setCopiaECola(response.copiaECola());
        cobranca.setDataExpiracao(
                response.dataExpiracao()
                        .withZoneSameInstant(ZONE_SAO_PAULO)
                        .toLocalDateTime()
        );
    }
}
