
package com.flightwatcher.subscription;

import com.flightwatcher.subscription.AlertCheckService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FlightPriceScheduler {

    private final AlertCheckService alertCheckService;

    public FlightPriceScheduler(AlertCheckService alertCheckService) {
        this.alertCheckService = alertCheckService;
    }

    @Scheduled(fixedDelayString = "${flightwatcher.check-interval-ms:1800000}")
    public void checkPrices() {
        System.out.println("Starting scheduled flight price check...");

        alertCheckService.checkAllAlerts();

        System.out.println("Scheduled flight price check finished.");
    }
}
