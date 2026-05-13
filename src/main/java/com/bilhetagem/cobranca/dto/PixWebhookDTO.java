package com.bilhetagem.cobranca.dto;

import java.util.List;

public record PixWebhookDTO(
        List<PixItemDTO> pix
) {}
