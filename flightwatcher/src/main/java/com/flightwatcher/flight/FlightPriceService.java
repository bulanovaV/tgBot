package com.flightwatcher.flight;

import com.flightwatcher.flight.FlightPriceClient;
import com.flightwatcher.flight.dto.FlightOffer;
import com.flightwatcher.flight.dto.FlightPriceResponse;
import com.flightwatcher.subscription.PriceAlert;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

@Service
public class FlightPriceService {
    private final FlightPriceClient flightPriceClient;

    public FlightPriceService(FlightPriceClient flightPriceClient){
        this.flightPriceClient = flightPriceClient;
    }
    public Optional<FlightOffer> findCheapestFlight(PriceAlert alert) {
        FlightPriceResponse response = flightPriceClient.getPrices(
                alert.getOrigin(),
                alert.getDestination(),
                alert.getDepartureDate()
        );

        if (response == null || !response.success()) {
            throw new IllegalStateException("Failed to fetch flight prices");
        }

        if (response.data() == null || response.data().isEmpty()) {
            return Optional.empty();
        }

        return response.data().stream()
                .min(Comparator.comparingInt(FlightOffer::price));
    }
}
