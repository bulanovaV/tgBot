package com.flightwatcher.flight.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record FlightOffer(
        int price,
        String airline,
        String origin,
        String destination,
        int transfers,
        @JsonProperty("departure_at") OffsetDateTime departureAt
){

}