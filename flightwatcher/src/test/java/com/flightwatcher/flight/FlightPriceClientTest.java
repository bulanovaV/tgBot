package com.flightwatcher.flight;

import com.flightwatcher.flight.dto.FlightOffer;
import com.flightwatcher.flight.dto.FlightPriceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
class FlightPriceClientTest {

    @Autowired
    private FlightPriceClient client;

    @Test
    void getTestDataTest() {
        FlightPriceResponse response = client.getPrices("MOW", "LED", LocalDate.of(2026, 10, 24));

        for (FlightOffer cur: response.data()){
            System.out.println(cur.price());
        }
    }
}