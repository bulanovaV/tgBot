package com.flightwatcher.subscription.dto;

import java.time.LocalDate;
import java.util.Objects;

public record AlertResponse(
        Long id,
        String origin,
        String destination,
        Integer maxPrice,
        LocalDate departureDate
) {
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        AlertResponse response = (AlertResponse) object;
        return Objects.equals(id, response.id) && Objects.equals(origin, response.origin) && Objects.equals(maxPrice, response.maxPrice) && Objects.equals(destination, response.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, origin, destination, maxPrice);
    }
}
