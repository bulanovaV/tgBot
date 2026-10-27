package com.flightwatcher.subscription;

import com.flightwatcher.subscription.dto.AlertResponse;
import com.flightwatcher.subscription.dto.CreateAlertRequest;
import com.flightwatcher.subscription.dto.EditAlertRequest;
import com.flightwatcher.subscription.AlertService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AlertController {

    private final AlertService alertService;
    public AlertController(AlertService alertService){
        this.alertService = alertService;
    }

    @GetMapping("/api/alerts")
    public List<AlertResponse> findAlerts(){
        return alertService.findAllAlerts();
    }

    @GetMapping("/api/alerts/{id}")
    public AlertResponse findAlert(@Valid @PathVariable Long id){
        return alertService.findAlert(id);
    }

    @DeleteMapping("/api/alerts/{id}")
    public void deleteAlert(@Valid @PathVariable Long id){
        alertService.deleteAlert(id);
    }

    @PatchMapping("/api/alerts/{id}")
    public AlertResponse editAlert(@PathVariable Long id, @Valid @RequestBody EditAlertRequest request){
        return alertService.editAlert(id, request);
    }
}