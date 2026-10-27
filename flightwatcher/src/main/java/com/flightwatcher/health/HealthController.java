package com.flightwatcher.health;

import com.flightwatcher.health.HealthResponse;
import com.flightwatcher.health.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/api/health")
    public HealthResponse health() {
        return healthService.getStatus();
    }
}