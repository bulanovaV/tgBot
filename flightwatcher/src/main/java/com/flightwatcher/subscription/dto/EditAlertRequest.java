package com.flightwatcher.subscription.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record EditAlertRequest (
        @Pattern(regexp = "[A-Z]{3}") String origin,
        @Pattern(regexp = "[A-Z]{3}")String destination,
        @Positive Integer maxPrice,
        @FutureOrPresent LocalDate departureDate
)
{
}
