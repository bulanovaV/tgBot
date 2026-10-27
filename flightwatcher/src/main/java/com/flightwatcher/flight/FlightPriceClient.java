package com.flightwatcher.flight;


import com.flightwatcher.flight.dto.FlightPriceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;

@Component
public class FlightPriceClient {

    private final RestClient restClient;

    public FlightPriceClient(RestClient.Builder builder, @Value("${travelpayouts.token}") String token) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = builder.clone()
                .baseUrl("https://api.travelpayouts.com")
                .defaultHeader("X-Access-Token", token)
                .requestFactory(requestFactory)
                .build();
    }

    public FlightPriceResponse getPrices(
            String origin,
            String destination,
            LocalDate departureDate
    ) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/aviasales/v3/prices_for_dates")
                        .queryParam("origin", origin)
                        .queryParam("destination", destination)
                        .queryParam("departure_at", departureDate)
                        .queryParam("currency", "rub")
                        .queryParam("market", "ru")
                        .queryParam("one_way", true)
                        .queryParam("direct", false)
                        .queryParam("unique", false)
                        .queryParam("limit", 10)
                        .queryParam("sorting", "price")
                        .build())
                .retrieve()
                .body(FlightPriceResponse.class);
    }
}
