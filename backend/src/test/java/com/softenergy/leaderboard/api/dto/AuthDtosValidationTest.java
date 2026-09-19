package com.softenergy.leaderboard.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtosValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void miniappLoginDoesNotRequireDevelopmentDeviceId() {
        var request = new AuthDtos.MiniappLoginRequest(
                "wechat-login-code", null, true, "2026-08-07", "2026-08-07");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void miniappLoginStillRequiresAgreementConsent() {
        var request = new AuthDtos.MiniappLoginRequest(
                "wechat-login-code", null, false, "2026-08-07", "2026-08-07");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("agreementAccepted");
    }
}
