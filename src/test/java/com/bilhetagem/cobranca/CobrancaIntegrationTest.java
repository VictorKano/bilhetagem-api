package com.bilhetagem.cobranca;

import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CobrancaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @Test
    void fluxoACriarCobrancaEConsultar() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "valor", "50.00",
                "tipo", CobrancaTipoEnum.RECARGA.name(),
                "metodo", CobrancaMetodoEnum.PIX.name()
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/cobrancas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.txid").isNotEmpty())
                .andExpect(jsonPath("$.copiaECola").isNotEmpty())
                .andExpect(jsonPath("$.dataExpiracao").isNotEmpty())
                .andReturn();
        String createResponseBody = createResult.getResponse().getContentAsString();
        Map<?, ?> createResponse = objectMapper.readValue(createResponseBody, Map.class);
        Long id = ((Number) createResponse.get("id")).longValue();
        String txid = (String) createResponse.get("txid");
        String copiaECola = (String) createResponse.get("copiaECola");

        assertThat(id).isNotNull().isPositive();
        assertThat(txid).isNotBlank();
        assertThat(copiaECola).isNotBlank();
        mockMvc.perform(get("/api/v1/cobrancas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.txid").value(txid))
                .andExpect(jsonPath("$.idUsuario").value("user-123"))
                .andExpect(jsonPath("$.tipo").value(CobrancaTipoEnum.RECARGA.name()))
                .andExpect(jsonPath("$.metodo").value(CobrancaMetodoEnum.PIX.name()))
                .andExpect(jsonPath("$.status").value(CobrancaStatusEnum.SOLICITADA.name()))
                .andExpect(jsonPath("$.valorSolicitado").value(50.00))
                .andExpect(jsonPath("$.dataCriacao").isNotEmpty());
    }

    
    @Test
    void fluxoAConsultarCobrancaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/v1/cobrancas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    
    @Test
    void fluxoBWebhookPixFinalizaCobrancaEPersistValorPago() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "valor", "120.00",
                "tipo", CobrancaTipoEnum.RECARGA.name(),
                "metodo", CobrancaMetodoEnum.PIX.name()
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/cobrancas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> createResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), Map.class);
        Long id = ((Number) createResponse.get("id")).longValue();
        String txid = (String) createResponse.get("txid");

        assertThat(txid).isNotBlank();
        BigDecimal valorPago = new BigDecimal("120.00");
        Map<String, Object> webhookBody = Map.of(
                "pix", List.of(Map.of(
                        "txid", txid,
                        "valor", valorPago,
                        "endToEndId", "E00000000000000000000000001"
                ))
        );

        mockMvc.perform(post("/api/v1/cobrancas/webhook/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookBody)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/cobrancas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CobrancaStatusEnum.FINALIZADA.name()))
                .andExpect(jsonPath("$.valorPago").value(120.00))
                .andExpect(jsonPath("$.dataFinalizada").isNotEmpty());
    }

    
    @Test
    void fluxoBWebhookPixTxidInexistenteRetorna200SemErro() throws Exception {
        Map<String, Object> webhookBody = Map.of(
                "pix", List.of(Map.of(
                        "txid", "txid-que-nao-existe-99999",
                        "valor", new BigDecimal("10.00"),
                        "endToEndId", "E00000000000000000000000002"
                ))
        );

        mockMvc.perform(post("/api/v1/cobrancas/webhook/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookBody)))
                .andExpect(status().isOk());
    }
}
