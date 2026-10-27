package com.flightwatcher.subscription.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateAlertRequest(
        @NotNull @Pattern(regexp = "[A-Z]{3}") String origin,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String destination,
        @NotNull @Positive Integer maxPrice,
        @NotNull @FutureOrPresent LocalDate departureDate
) {
}