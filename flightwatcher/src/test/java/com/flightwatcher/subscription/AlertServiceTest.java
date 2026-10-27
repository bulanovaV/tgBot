package com.flightwatcher.subscription;


import com.flightwatcher.subscription.dto.AlertResponse;
import com.flightwatcher.subscription.dto.CreateAlertRequest;
import com.flightwatcher.subscription.dto.EditAlertRequest;
import com.flightwatcher.subscription.PriceAlert;
import com.flightwatcher.common.exception.AlertNotFoundException;
import com.flightwatcher.subscription.PriceAlertRepository;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

    @Mock
    private PriceAlertRepository repository;

    @InjectMocks
    private AlertService alertService;

    @Test
    void findAlertSuccess() {
        Long id = 10L;
        PriceAlert alert = new PriceAlert("BER", "IST", 150, LocalDate.of(2026, 10, 10));
        when(repository.findById(id)).thenReturn(Optional.of(alert));
        AlertResponse res = alertService.findAlert(id);
        assertEquals("BER", res.origin());
        assertEquals("IST", res.destination());
        assertEquals(150, res.maxPrice());
        assertEquals(LocalDate.of(2026, 10, 10), res.departureDate());
    }

    @Test
    void alertNotFound() {
        Long id = 10L;
        when(repository.findById(id)).thenReturn(Optional.empty());
        AlertNotFoundException exc = assertThrows(AlertNotFoundException.class, () -> alertService.findAlert(id));
        assertEquals(exc.getMessage(), "Alert with id = " + id + " not found");
    }

    @Test
    void createAlertSuccess() {
        CreateAlertRequest request = new CreateAlertRequest("BER", "IST", 150, LocalDate.of(2026, 10, 10));
        PriceAlert alert = new PriceAlert("BER", "IST", 150, LocalDate.of(2026, 10, 10));
        when(repository.save(any(PriceAlert.class))).thenReturn(alert);
        AlertResponse response = alertService.createAlert(request);
        verify(repository).save(any(PriceAlert.class));
        assertEquals(response.origin(), "BER");
        assertEquals(response.destination(), "IST");
        assertEquals(response.maxPrice(), 150);
        assertEquals(LocalDate.of(2026, 10, 10), response.departureDate());
    }

    @Test
    void createAlertCheckArgsTest() {
        CreateAlertRequest request = new CreateAlertRequest("BER", "IST", 150, LocalDate.of(2026, 10, 10));
        PriceAlert alert = new PriceAlert("BER", "IST", 150, LocalDate.of(2026, 10, 10));
        ArgumentCaptor<PriceAlert> captor = ArgumentCaptor.forClass(PriceAlert.class);
        when(repository.save(any(PriceAlert.class))).thenReturn(alert);
        AlertResponse response = alertService.createAlert(request);
        verify(repository).save(captor.capture());
        PriceAlert arg = captor.getValue();
        assertEquals(response.origin(), arg.getOrigin());
        assertEquals(response.destination(), arg.getDestination());
        assertEquals(response.maxPrice(), arg.getMaxPrice());
        assertEquals(response.departureDate(), arg.getDepartureDate());
    }


    @Test
    void deleteFoundAlertTest() {
        Long id = 10L;
        PriceAlert alert = new PriceAlert("BER", "IST", 150, LocalDate.of(2026, 10, 10));
        when(repository.findById(id)).thenReturn(Optional.of(alert));
        alertService.deleteAlert(id);
        verify(repository).delete(alert);
    }

    @Test
    void deleteNotFoundAlertTest() {
        Long id = 10L;
        when(repository.findById(id)).thenReturn(Optional.empty());
        AlertNotFoundException exc = assertThrows(AlertNotFoundException.class, () -> alertService.deleteAlert(id));
        assertEquals(exc.getMessage(), "Alert with id = " + id + " not found");
        verify(repository, never()).delete(any(PriceAlert.class));
    }

    @Test
    void findAllAlertsTest() {
        PriceAlert alert1 = new PriceAlert("BER", "IST", 150, LocalDate.of(2026, 10, 10));
        PriceAlert alert2 = new PriceAlert("FRA", "PAR", 200, LocalDate.of(2026, 10, 10));
        when(repository.findAll()).thenReturn(List.of(alert1, alert2));
        List<AlertResponse> arr = alertService.findAllAlerts();
        verify(repository).findAll();
        assertEquals(arr.size(), 2);
        assertEqualsPrAlAndAlResp(alert1, arr.getFirst());
        assertEqualsPrAlAndAlResp(alert2, arr.get(1));
    }


    @Test
    void findAllAlertsEmpty() {
        when(repository.findAll()).thenReturn(List.of());
        List<AlertResponse> arr = alertService.findAllAlerts();
        verify(repository).findAll();
        assertTrue(arr.isEmpty());
    }


    @Test
    void editAlertTest() {
        Long id = 10L;
        PriceAlert alert = new PriceAlert("BER", "IST", 150, LocalDate.of(2026, 10, 10));
        EditAlertRequest request = new EditAlertRequest("FRA", "GER", 200, LocalDate.of(2026, 10, 10));
        when(repository.findById(id)).thenReturn(Optional.of(alert));
        when(repository.save(alert)).thenReturn(alert);
        AlertResponse response = alertService.editAlert(id, request);
        assertEquals("FRA", response.origin());
        assertEquals("GER", response.destination());
        assertEquals(200, response.maxPrice());
        assertEquals(LocalDate.of(2026, 10, 10), response.departureDate());
    }

    @Test
    void editPartOfAlertTest() {
        Long id = 10L;
        PriceAlert alert = new PriceAlert("BER", "IST", 150, LocalDate.of(2026, 10, 10));
        EditAlertRequest request = new EditAlertRequest(null, null, 200, LocalDate.of(2026, 10, 10));
        when(repository.findById(id)).thenReturn(Optional.of(alert));
        when(repository.save(alert)).thenReturn(alert);
        AlertResponse response = alertService.editAlert(id, request);
        assertEquals("BER", response.origin());
        assertEquals("IST", response.destination());
        assertEquals(200, response.maxPrice());
        assertEquals(LocalDate.of(2026, 10, 10), response.departureDate());
    }

    @Test
    void editNotFoundAlertTest() {
        Long id = 10L;
        EditAlertRequest request = new EditAlertRequest(null, null, 200, LocalDate.of(2026, 10, 10));
        when(repository.findById(id)).thenReturn(Optional.empty());
        AlertNotFoundException exc = assertThrows(AlertNotFoundException.class,
                () -> alertService.editAlert(id, request));
        assertEquals(exc.getMessage(), "Alert with id = " + id + " not found");
        verify(repository, never()).save(any(PriceAlert.class));
    }

    private void assertEqualsPrAlAndAlResp(PriceAlert alert, AlertResponse response) {
        assertEquals(alert.getOrigin(), response.origin());
        assertEquals(alert.getDestination(), response.destination());
        assertEquals(alert.getMaxPrice(), response.maxPrice());
    }
}
