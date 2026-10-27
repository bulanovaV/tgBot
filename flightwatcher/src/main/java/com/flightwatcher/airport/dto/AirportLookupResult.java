package com.flightwatcher.airport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AirportLookupResult(
        String type,
        String code,
        String name,
        @JsonProperty("city_name") String cityName,
        @JsonProperty("country_name") String countryName
) {
}
