package com.flightwatcher.subscription.dto;

import com.flightwatcher.subscription.dto.CreateAlertRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class CreateAlertRequestValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldRejectPastDepartureDate() {
        CreateAlertRequest request = new CreateAlertRequest(
                "MOW",
                "LED",
                5000,
                LocalDate.now().minusDays(1)
        );

        Set<ConstraintViolation<CreateAlertRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
    }
}
