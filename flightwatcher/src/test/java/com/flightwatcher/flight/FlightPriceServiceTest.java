
package com.flightwatcher.flight;

import com.flightwatcher.flight.FlightPriceClient;
import com.flightwatcher.flight.dto.FlightOffer;
import com.flightwatcher.flight.dto.FlightPriceResponse;
import com.flightwatcher.subscription.PriceAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlightPriceServiceTest {

    private FlightPriceClient flightPriceClient;
    private FlightPriceService flightPriceService;
    private PriceAlert alert;

    @BeforeEach
    void setUp() {
        flightPriceClient = mock(FlightPriceClient.class);
        flightPriceService = new FlightPriceService(flightPriceClient);
        alert = new PriceAlert();
        alert.setOrigin("MOW");
        alert.setDestination("LED");
        alert.setDepartureDate(LocalDate.of(2026, 10, 30));
        alert.setMaxPrice(3000);
    }

    @Test
    void shouldReturnCheapestMatchingFlight() {
        FlightOffer expensive = new FlightOffer(
                4500, "S7", "MOW", "LED", 0, OffsetDateTime.parse("2026-10-30T17:20:00+03:00")
        );
        FlightOffer cheap = new FlightOffer(
                2500, "SU", "MOW", "LED", 0, OffsetDateTime.parse("2026-10-30T17:20:00+03:00")
        );
        FlightOffer medium = new FlightOffer(
                2800, "DP", "MOW", "LED", 1, OffsetDateTime.parse("2026-10-30T17:20:00+03:00")
        );

        FlightPriceResponse response = new FlightPriceResponse(
                List.of(expensive, cheap, medium),
                "rub",
                true
        );

        when(flightPriceClient.getPrices(
                "MOW", "LED", alert.getDepartureDate()
        )).thenReturn(response);

        Optional<FlightOffer> result =
                flightPriceService.findCheapestFlight(alert);

        assertTrue(result.isPresent());
        assertEquals(2500, result.get().price());

        verify(flightPriceClient).getPrices(
                "MOW", "LED", alert.getDepartureDate()
        );
    }

    @Test
    void shouldReturnEmptyWhenAllFlightsAreTooExpensive() {
        FlightOffer first = new FlightOffer(
                4500, "S7", "MOW", "LED", 0, OffsetDateTime.parse("2026-10-30T17:20:00+03:00")
        );
        FlightOffer second = new FlightOffer(
                5200, "SU", "MOW", "LED", 1, OffsetDateTime.parse("2026-10-30T17:20:00+03:00")
        );

        FlightPriceResponse response = new FlightPriceResponse(
                List.of(first, second),
                "rub",
                true
        );

        when(flightPriceClient.getPrices(
                "MOW", "LED", alert.getDepartureDate()
        )).thenReturn(response);

        Optional<FlightOffer> result =
                flightPriceService.findCheapestFlight(alert);

        assertTrue(result.isPresent());
        assertEquals(4500, result.get().price());
    }

    @Test
    void shouldReturnEmptyWhenApiReturnsNoFlights() {
        FlightPriceResponse response = new FlightPriceResponse(
                List.of(),
                "rub",
                true
        );

        when(flightPriceClient.getPrices(
                "MOW", "LED", alert.getDepartureDate()
        )).thenReturn(response);

        Optional<FlightOffer> result =
                flightPriceService.findCheapestFlight(alert);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenApiRequestFails() {
        FlightPriceResponse response = new FlightPriceResponse(
                List.of(),
                "rub",
                false
        );

        when(flightPriceClient.getPrices(
                "MOW", "LED", alert.getDepartureDate()
        )).thenReturn(response);

        assertThrows(
                IllegalStateException.class,
                () -> flightPriceService.findCheapestFlight(alert)
        );
    }

    @Test
    void shouldThrowExceptionWhenApiReturnsNull() {
        when(flightPriceClient.getPrices(
                "MOW", "LED", alert.getDepartureDate()
        )).thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> flightPriceService.findCheapestFlight(alert)
        );
    }
}
