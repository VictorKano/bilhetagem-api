package com.bilhetagem.cobranca.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "local"})
public class FakeUserContext implements UserContext {

    @Override
    public String getIdUsuario() {
        return "user-123";
    }

    @Override
    public String getGivenName() {
        return "João";
    }

    @Override
    public String getFamilyName() {
        return "Silva";
    }

    @Override
    public String getCpf() {
        return "12345678901";
    }
}
