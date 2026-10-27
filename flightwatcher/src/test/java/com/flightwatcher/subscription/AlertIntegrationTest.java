package com.flightwatcher.subscription;

import com.flightwatcher.subscription.dto.AlertResponse;
import com.flightwatcher.subscription.dto.CreateAlertRequest;
import com.flightwatcher.subscription.dto.EditAlertRequest;
import com.flightwatcher.subscription.PriceAlert;
import com.flightwatcher.common.exception.AlertNotFoundException;
import com.flightwatcher.subscription.PriceAlertRepository;
import com.flightwatcher.subscription.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class AlertIntegrationTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private PriceAlertRepository repository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Test
    void contextLoads() {
    }


    @Test
    void createAlertTest() {
        CreateAlertRequest request = new CreateAlertRequest("MOW", "LED", 5000, LocalDate.of(2026, 10, 10));
        AlertResponse alert = alertService.createAlert(request);
        PriceAlert saveAlert = repository.findById(alert.id()).orElseThrow();
        assertEquals("MOW", saveAlert.getOrigin());
        assertEquals("LED", saveAlert.getDestination());
        assertEquals(5000, saveAlert.getMaxPrice());
        assertEquals(LocalDate.of(2026, 10, 10), saveAlert.getDepartureDate());
    }

    @Test
    void createAlertsTest() {
        CreateAlertRequest request1 = new CreateAlertRequest("MOW", "LED", 5000, LocalDate.of(2026, 10, 10));
        CreateAlertRequest request2 = new CreateAlertRequest("DME", "PRA", 1000, LocalDate.of(2026, 10, 10));
        AlertResponse alert1 = alertService.createAlert(request1);
        AlertResponse alert2 = alertService.createAlert(request2);
        PriceAlert saveAlert1 = repository.findById(alert1.id()).orElseThrow();
        PriceAlert saveAlert2 = repository.findById(alert2.id()).orElseThrow();
        assertNotEquals(alert1.id(), alert2.id());
        assertEqualsPrAlAndAlResp(saveAlert1, alert1);
        assertEqualsPrAlAndAlResp(saveAlert2, alert2);
    }


    @Test
    void deleteAlertTest() {
        PriceAlert alert = repository.save(new PriceAlert("DME", "PRA", 1000, LocalDate.of(2026, 10, 10)));
        alertService.deleteAlert(alert.getId());
        AlertNotFoundException alertExc = assertThrows(AlertNotFoundException.class, () -> alertService.deleteAlert(alert.getId()));
        assertEquals(alertExc.getMessage(), "Alert with id = " + alert.getId() + " not found");
    }

    @Test
    void findAlertTest() {
        PriceAlert alert = repository.save(new PriceAlert("DME", "PRA", 1000, LocalDate.of(2026, 10, 10)));
        AlertResponse response = alertService.findAlert(alert.getId());
        assertEquals("DME", response.origin());
        assertEquals("PRA", response.destination());
        assertEquals(1000, response.maxPrice());
        assertEquals(LocalDate.of(2026, 10, 10), response.departureDate());
    }

    @Test
    void findAlertNotFoundTest() {
        Long id = 10L;
        AlertNotFoundException exception = assertThrows(AlertNotFoundException.class, () -> alertService.findAlert(id));
        assertEquals("Alert with id = " + id + " not found", exception.getMessage());
    }

    @Test
    void findAllAlertTest() {
        PriceAlert alert1 = repository.save(new PriceAlert("DME", "PRA", 1000, LocalDate.of(2026, 10, 10)));
        PriceAlert alert2 = repository.save(new PriceAlert("PER", "GER", 2000, LocalDate.of(2026, 10, 10)));
        PriceAlert alert3 = repository.save(new PriceAlert("LED", "MOS", 3000, LocalDate.of(2026, 10, 10)));
        List<AlertResponse> alerts = alertService.findAllAlerts();
        assertEquals(alerts.size(), 3);
        assertTrue(alerts.stream()
                .anyMatch(a -> a.id().equals(alert1.getId())));
        assertTrue(alerts.stream()
                .anyMatch(a -> a.id().equals(alert2.getId())));
        assertTrue(alerts.stream()
                .anyMatch(a -> a.id().equals(alert3.getId())));
    }

    @Test
    void findEmptyAlertTest() {
        List<AlertResponse> alerts = alertService.findAllAlerts();
        assertEquals(0, alerts.size());
    }

    @Test
    void editAlertTest() {
        PriceAlert alert = repository.save(new PriceAlert("DME", "PRA", 1000, LocalDate.of(2026, 10, 10)));
        EditAlertRequest request = new EditAlertRequest("GER", "MOS", 2000, LocalDate.of(2026, 10, 10));
        AlertResponse editAlert = alertService.editAlert(alert.getId(), request);
        assertEquals("GER", editAlert.origin());
        assertEquals("MOS", editAlert.destination());
        assertEquals(2000, editAlert.maxPrice());
        assertEquals(LocalDate.of(2026, 10, 10), editAlert.departureDate());
    }

    @Test
    void editPartOfAlertTest() {
        PriceAlert alert = repository.save(new PriceAlert("DME", "PRA", 1000, LocalDate.of(2026, 10, 10)));
        EditAlertRequest request = new EditAlertRequest(null, null, 2000, LocalDate.of(2026, 10, 10));
        AlertResponse editAlert = alertService.editAlert(alert.getId(), request);
        assertEquals("DME", editAlert.origin());
        assertEquals("PRA", editAlert.destination());
        assertEquals(2000, editAlert.maxPrice());
        assertEquals(LocalDate.of(2026, 10, 10), editAlert.departureDate());
    }

    @Test
    void editNotFoundAlertTest() {
        Long id = 10L;
        EditAlertRequest request = new EditAlertRequest(null, null, 2000, LocalDate.of(2026, 10, 10));
        AlertNotFoundException exception = assertThrows(AlertNotFoundException.class, () -> alertService.editAlert(id, request));
        assertEquals("Alert with id = " + id + " not found", exception.getMessage());
    }

    @Test
    void deleteNotFoundAlertTest() {
        Long id = 10L;
        AlertNotFoundException exception = assertThrows(AlertNotFoundException.class, () -> alertService.deleteAlert(id));
        assertEquals("Alert with id = " + id + " not found", exception.getMessage());
    }

    @Test
    void checkIdAlertsTest() {
        CreateAlertRequest alert1 = new CreateAlertRequest("DME", "GER", 5000, LocalDate.of(2026, 10, 10));
        CreateAlertRequest alert2 = new CreateAlertRequest("DME", "GER", 5000, LocalDate.of(2026, 10, 10));
        AlertResponse response1 = alertService.createAlert(alert1);
        AlertResponse response2 = alertService.createAlert(alert2);
        assertNotEquals(response1.id(), response2.id());
    }

    private void assertEqualsPrAlAndAlResp(PriceAlert alert, AlertResponse response) {
        assertEquals(alert.getOrigin(), response.origin());
        assertEquals(alert.getDestination(), response.destination());
        assertEquals(alert.getMaxPrice(), response.maxPrice());
        assertEquals(alert.getDepartureDate(), response.departureDate());
    }

}