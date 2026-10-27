package com.flightwatcher.health;

public record HealthResponse(
        String status,
        String application
) {
}