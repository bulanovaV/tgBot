package com.flightwatcher.airport;

import com.flightwatcher.airport.AirportCatalogService;
import com.flightwatcher.airport.AirportLookupClient;
import org.springframework.stereotype.Service;

@Service
public class AirportDisplayService {

    private final AirportCatalogService airportCatalog;
    private final AirportLookupClient airportLookupClient;

    public AirportDisplayService(
            AirportCatalogService airportCatalog,
            AirportLookupClient airportLookupClient
    ) {
        this.airportCatalog = airportCatalog;
        this.airportLookupClient = airportLookupClient;
    }

    public String getCityName(String airportCode) {
        if (airportCatalog.findAirport(airportCode).isPresent()) {
            return airportCatalog.getCityName(airportCode);
        }

        try {
            return airportLookupClient.findAirport(airportCode)
                    .map(airport -> {
                        airportCatalog.registerAirport(
                                airport.code(),
                                airport.name(),
                                airport.cityName()
                        );
                        return airportCatalog.getCityName(airport.code());
                    })
                    .orElse(airportCode);
        } catch (Exception e) {
            return airportCode;
        }
    }
}
