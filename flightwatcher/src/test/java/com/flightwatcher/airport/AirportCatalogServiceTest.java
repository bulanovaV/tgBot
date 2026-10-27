package com.flightwatcher.airport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirportCatalogServiceTest {

    private final AirportCatalogService catalog = new AirportCatalogService();

    @Test
    void shouldLoadFifteenCountriesWithNoMoreThanTenAirports() {
        assertEquals(15, catalog.getCountries().size());
        assertTrue(catalog.getCountries().stream()
                .allMatch(country -> !country.airports().isEmpty()
                        && country.airports().size() <= 10));
    }

    @Test
    void shouldFindAirportOnlyInsideSelectedCountry() {
        assertTrue(catalog.findAirport("RU", "SVO").isPresent());
        assertTrue(catalog.findAirport("TR", "SVO").isEmpty());
    }
}
