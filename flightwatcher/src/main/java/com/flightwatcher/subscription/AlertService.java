package com.flightwatcher.subscription;

import com.flightwatcher.subscription.dto.AlertResponse;
import com.flightwatcher.subscription.dto.CreateAlertRequest;
import com.flightwatcher.subscription.dto.EditAlertRequest;
import com.flightwatcher.subscription.PriceAlert;
import com.flightwatcher.common.exception.AlertNotFoundException;
import com.flightwatcher.subscription.PriceAlertRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {
    private final PriceAlertRepository repository;

    public AlertService(PriceAlertRepository repository) {
        this.repository = repository;
    }

    public AlertResponse createAlert(CreateAlertRequest request) {
        PriceAlert alert = new PriceAlert(request.origin(), request.destination(), request.maxPrice(), request.departureDate());
        PriceAlert dbAlert = repository.save(alert);
        return castToAlertResponse(dbAlert);
    }

    public List<AlertResponse> findAllAlerts() {
        List<PriceAlert> priceAlerts = repository.findAll();
        List<AlertResponse> responses = new ArrayList<>();
        for (PriceAlert priceAlert : priceAlerts) {
            responses.add(castToAlertResponse(priceAlert));
        }
        return responses;
    }

    public AlertResponse findAlert(Long id) {
        Optional<PriceAlert> alert = repository.findById(id);
        return alert.map(this::castToAlertResponse).orElseThrow(() -> new AlertNotFoundException(id));
    }


    public void deleteAlert(Long id) {
        Optional<PriceAlert> alert = repository.findById(id);
        if (alert.isPresent()) repository.delete(alert.get());
        else {
            throw new AlertNotFoundException(id);
        }
    }

    public AlertResponse editAlert(Long id, EditAlertRequest request) throws AlertNotFoundException {
        PriceAlert curAlert = repository.findById(id).orElseThrow(() -> new AlertNotFoundException(id));
        if (request.origin() != null) {
            curAlert.setOrigin(request.origin());
        }
        if (request.destination() != null) {
            curAlert.setDestination(request.destination());
        }
        if (request.maxPrice() != null) {
            curAlert.setMaxPrice(request.maxPrice());
        }
        PriceAlert dbAlert = repository.save(curAlert);
        return castToAlertResponse(dbAlert);
    }

    private AlertResponse castToAlertResponse(PriceAlert dbAlert) {
        return new AlertResponse(dbAlert.getId(), dbAlert.getOrigin(), dbAlert.getDestination(), dbAlert.getMaxPrice(), dbAlert.getDepartureDate());
    }

}
