package com.flightwatcher.airport;

import com.flightwatcher.airport.dto.AirportLookupResult;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Component
public class AirportLookupClient {

    private final RestClient restClient;

    public AirportLookupClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = builder.clone()
                .baseUrl("https://autocomplete.travelpayouts.com")
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<AirportLookupResult> findAirport(String iataCode) {
        String normalizedCode = iataCode.toUpperCase(Locale.ROOT);

        AirportLookupResult[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/places2")
                        .queryParam("term", normalizedCode)
                        .queryParam("locale", "ru")
                        .queryParam("types[]", "airport", "city")
                        .build())
                .retrieve()
                .body(AirportLookupResult[].class);

        if (response == null) {
            return Optional.empty();
        }

        return Arrays.stream(response)
                .filter(result -> "airport".equals(result.type())
                        || "city".equals(result.type()))
                .filter(result -> normalizedCode.equalsIgnoreCase(result.code()))
                .findFirst();
    }
}
