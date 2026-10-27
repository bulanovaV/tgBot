package com.flightwatcher.subscription.dto;

import com.flightwatcher.subscription.dto.EditAlertRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

class EditAlertRequestValidationTest {

    private static final ValidatorFactory factory =
            Validation.buildDefaultValidatorFactory();

    private final Validator validator = factory.getValidator();

    @Test
    void shouldAcceptNullDepartureDate() {
        EditAlertRequest request = new EditAlertRequest(
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<EditAlertRequest>> violations =
                validator.validate(request);

        Assertions.assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectPastDepartureDate() {
        EditAlertRequest request = new EditAlertRequest(
                null,
                null,
                null,
                LocalDate.now().minusDays(1)
        );

        Set<ConstraintViolation<EditAlertRequest>> violations =
                validator.validate(request);

        Assertions.assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath()
                                .toString()
                                .equals("departureDate"))
        );
    }

    @Test
    void shouldAcceptFutureDepartureDate() {
        EditAlertRequest request = new EditAlertRequest(
                null,
                null,
                null,
                LocalDate.now().plusDays(10)
        );

        Set<ConstraintViolation<EditAlertRequest>> violations =
                validator.validate(request);

        Assertions.assertTrue(violations.isEmpty());
    }
}