package com.bilhetagem.cobranca.service;

import com.bilhetagem.cobranca.domain.CobrancaMetodoEnum;
import com.bilhetagem.cobranca.domain.CobrancaStatusEnum;
import com.bilhetagem.cobranca.domain.CobrancaTipoEnum;
import com.bilhetagem.cobranca.dto.CobrancaCompletoResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class CobrancaSerializacaoTest {

    private static final Pattern ISO_SEM_OFFSET = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
        return mapper;
    }

    private String extractJsonStringField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    @Test
    void localDateTimeSerializacaoSemOffset() throws Exception {
        ObjectMapper mapper = buildObjectMapper();
        LocalDateTime dataHora = LocalDateTime.of(2026, 5, 13, 10, 30, 0);
        String json = mapper.writeValueAsString(dataHora);
        String valor = json.replaceAll("^\"|\"$", "");
        assertThat(valor).matches(ISO_SEM_OFFSET).doesNotContain("+").doesNotContain("Z");
    }

    @Test
    void localDateTimeSerializacaoPreservaValor() throws Exception {
        ObjectMapper mapper = buildObjectMapper();
        LocalDateTime dataHora = LocalDateTime.of(2026, 5, 13, 10, 30, 45);
        String json = mapper.writeValueAsString(dataHora);
        String valor = json.replaceAll("^\"|\"$", "");
        assertThat(valor).isEqualTo(dataHora.format(FORMATTER));
    }

    @Test
    void cobrancaCompletoResponseDtoCamposDataSemOffset() throws Exception {
        ObjectMapper mapper = buildObjectMapper();
        LocalDateTime dataCriacao = LocalDateTime.of(2026, 1, 1, 8, 0, 0);
        LocalDateTime dataExpiracao = LocalDateTime.of(2026, 1, 1, 9, 0, 0);
        LocalDateTime dataFinalizada = LocalDateTime.of(2026, 1, 1, 8, 30, 0);

        CobrancaCompletoResponseDTO dto = new CobrancaCompletoResponseDTO(
                1L, "txid", "usuario", CobrancaTipoEnum.RECARGA, CobrancaMetodoEnum.PIX,
                CobrancaStatusEnum.FINALIZADA, BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                dataCriacao, dataExpiracao, dataFinalizada);

        String json = mapper.writeValueAsString(dto);

        assertThat(extractJsonStringField(json, "dataCriacao")).matches(ISO_SEM_OFFSET);
        assertThat(extractJsonStringField(json, "dataExpiracao")).matches(ISO_SEM_OFFSET);
        assertThat(extractJsonStringField(json, "dataFinalizada")).matches(ISO_SEM_OFFSET);
    }
}
