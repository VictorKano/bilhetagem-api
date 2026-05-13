package com.bilhetagem.cobranca.service;

import com.bilhetagem.cobranca.dto.CheckoutValidateRequestDTO;
import com.bilhetagem.cobranca.dto.CobrancaRequestDTO;
import com.bilhetagem.cobranca.integration.CheckoutValidationClient;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CobrancaValidacaoTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    @Test
    void valorNegativoDeveTerViolacaoNocampo() {
        CobrancaRequestDTO dto = new CobrancaRequestDTO(new BigDecimal("-1.00"), null, null);
        Set<ConstraintViolation<CobrancaRequestDTO>> violations = VALIDATOR.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> "valor".equals(v.getPropertyPath().toString()))).isTrue();
    }

    @Test
    void valorZeroDeveTerViolacaoNocampo() {
        CobrancaRequestDTO dto = new CobrancaRequestDTO(BigDecimal.ZERO, null, null);
        Set<ConstraintViolation<CobrancaRequestDTO>> violations = VALIDATOR.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> "valor".equals(v.getPropertyPath().toString()))).isTrue();
    }

    @Test
    void valorNuloDeveTerViolacaoNocamp() {
        CobrancaRequestDTO dto = new CobrancaRequestDTO(null, null, null);
        Set<ConstraintViolation<CobrancaRequestDTO>> violations = VALIDATOR.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> "valor".equals(v.getPropertyPath().toString()))).isTrue();
    }

    @Test
    void cavvNuloDeveTerViolacao() {
        CheckoutValidateRequestDTO dto = new CheckoutValidateRequestDTO(null, "xid-valido", "05");
        Set<ConstraintViolation<CheckoutValidateRequestDTO>> violations = VALIDATOR.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> "cavv".equals(v.getPropertyPath().toString()))).isTrue();

        CheckoutValidationClient client = mock(CheckoutValidationClient.class);
        verify(client, never()).validar(anyString(), any());
    }

    @Test
    void xidNuloDeveTerViolacao() {
        CheckoutValidateRequestDTO dto = new CheckoutValidateRequestDTO("cavv-valido", null, "05");
        Set<ConstraintViolation<CheckoutValidateRequestDTO>> violations = VALIDATOR.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> "xid".equals(v.getPropertyPath().toString()))).isTrue();
    }

    @Test
    void eciNuloDeveTerViolacao() {
        CheckoutValidateRequestDTO dto = new CheckoutValidateRequestDTO("cavv-valido", "xid-valido", null);
        Set<ConstraintViolation<CheckoutValidateRequestDTO>> violations = VALIDATOR.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> "eci".equals(v.getPropertyPath().toString()))).isTrue();
    }

    @Test
    void camposVaziosDevemTerViolacao() {
        assertThat(VALIDATOR.validate(new CheckoutValidateRequestDTO("", "xid", "05"))).isNotEmpty();
        assertThat(VALIDATOR.validate(new CheckoutValidateRequestDTO("cavv", "", "05"))).isNotEmpty();
        assertThat(VALIDATOR.validate(new CheckoutValidateRequestDTO("cavv", "xid", ""))).isNotEmpty();
    }
}
