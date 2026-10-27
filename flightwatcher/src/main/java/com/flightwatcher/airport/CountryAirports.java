package com.flightwatcher.airport;

import java.util.List;

public record CountryAirports(
        String code,
        String name,
        List<Airport> airports
) {
}
