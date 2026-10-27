package com.flightwatcher.airport;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AirportCatalogService {

    private static final int MAX_AIRPORTS_PER_COUNTRY = 10;
    private final List<CountryAirports> countries;
    private final Map<String, Airport> additionalAirports = new ConcurrentHashMap<>();
    private final Map<String, String> additionalCityNames = new ConcurrentHashMap<>();

    public AirportCatalogService() {
        this.countries = loadCatalog();
    }

    public List<CountryAirports> getCountries() {
        return countries;
    }

    public Optional<CountryAirports> findCountry(String countryCode) {
        return countries.stream()
                .filter(country -> country.code().equals(countryCode))
                .findFirst();
    }

    public Optional<Airport> findAirport(String countryCode, String airportCode) {
        return findCountry(countryCode)
                .flatMap(country -> country.airports().stream()
                        .filter(airport -> airport.code().equals(airportCode))
                        .findFirst());
    }

    public Optional<Airport> findAirport(String airportCode) {
        Optional<Airport> catalogAirport = countries.stream()
                .flatMap(country -> country.airports().stream())
                .filter(airport -> airport.code().equals(airportCode))
                .findFirst();

        return catalogAirport.isPresent()
                ? catalogAirport
                : Optional.ofNullable(additionalAirports.get(airportCode));
    }

    public String getCityName(String airportCode) {
        String additionalCityName = additionalCityNames.get(airportCode);
        if (additionalCityName != null) {
            return additionalCityName;
        }

        return findAirport(airportCode)
                .map(airport -> airport.name().split(" — ", 2)[0])
                .orElse(airportCode);
    }

    public void registerAirport(
            String code,
            String name,
            String cityName
    ) {
        additionalAirports.put(code, new Airport(code, name));
        if (cityName != null && !cityName.isBlank()) {
            additionalCityNames.put(code, cityName);
        }
    }

    private List<CountryAirports> loadCatalog() {
        Map<String, MutableCountry> loadedCountries = new LinkedHashMap<>();
        ClassPathResource resource = new ClassPathResource("catalog/airports.csv");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> addCatalogLine(loadedCountries, line));

        } catch (IOException e) {
            throw new IllegalStateException("Could not load airport catalog", e);
        }

        if (loadedCountries.size() != 15) {
            throw new IllegalStateException("Airport catalog must contain exactly 15 countries");
        }

        return loadedCountries.values().stream()
                .map(country -> new CountryAirports(
                        country.code,
                        country.name,
                        List.copyOf(country.airports)
                ))
                .toList();
    }

    private void addCatalogLine(Map<String, MutableCountry> countriesByCode, String line) {
        String[] columns = line.split(";", -1);

        if (columns.length != 4) {
            throw new IllegalStateException("Invalid airport catalog line: " + line);
        }

        String countryCode = columns[0].trim();
        String countryName = columns[1].trim();
        String airportCode = columns[2].trim();
        String airportName = columns[3].trim();

        MutableCountry country = countriesByCode.computeIfAbsent(
                countryCode,
                ignored -> new MutableCountry(countryCode, countryName)
        );

        if (!country.name.equals(countryName)) {
            throw new IllegalStateException("Different names for country " + countryCode);
        }

        if (country.airports.size() >= MAX_AIRPORTS_PER_COUNTRY) {
            throw new IllegalStateException(
                    "Country " + countryCode + " contains more than "
                            + MAX_AIRPORTS_PER_COUNTRY + " airports"
            );
        }

        country.airports.add(new Airport(airportCode, airportName));
    }

    private static final class MutableCountry {
        private final String code;
        private final String name;
        private final List<Airport> airports = new ArrayList<>();

        private MutableCountry(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
