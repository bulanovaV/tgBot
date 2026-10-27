package com.flightwatcher.flight.dto;

import java.util.List;

public record FlightPriceResponse(
        List<FlightOffer> data,
        String currency,
        boolean success
) {

}