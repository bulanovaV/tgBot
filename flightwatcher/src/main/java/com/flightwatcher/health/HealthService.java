package com.flightwatcher.health;

import com.flightwatcher.health.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getStatus() {
        return new HealthResponse("UP", "FlightWatcher");
    }
}