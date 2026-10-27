package com.flightwatcher.subscription;

import com.flightwatcher.flight.dto.FlightOffer;
import com.flightwatcher.flight.FlightPriceService;
import com.flightwatcher.telegram.TelegramNotificationService;
import com.flightwatcher.subscription.PriceAlert;
import com.flightwatcher.subscription.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertCheckServiceTest {

    private PriceAlertRepository repository;
    private FlightPriceService flightPriceService;
    private TelegramNotificationService notificationService;
    private AlertCheckService service;
    private PriceAlert alert;

    @BeforeEach
    void setUp() {
        repository = mock(PriceAlertRepository.class);
        flightPriceService = mock(FlightPriceService.class);
        notificationService = mock(TelegramNotificationService.class);
        service = new AlertCheckService(repository, flightPriceService, notificationService);

        alert = new PriceAlert("SVO", "IST", 15000, LocalDate.of(2027, 10, 27));
        alert.setTelegramChatId(123L);
        alert.setLastCheckedPrice(10000);
        when(repository.findAll()).thenReturn(List.of(alert));
    }

    @Test
    void shouldIgnorePriceChangeOfExactlyTwoHundredRubles() {
        when(flightPriceService.findCheapestFlight(alert))
                .thenReturn(Optional.of(offer(10200)));

        service.checkAllAlerts();

        verify(notificationService, never()).sendMessage(123L, anyString());
        verify(repository, never()).save(alert);
    }

    @Test
    void shouldNotifyWhenPriceChangesByMoreThanTwoHundredRubles() {
        when(flightPriceService.findCheapestFlight(alert))
                .thenReturn(Optional.of(offer(10201)));

        service.checkAllAlerts();

        verify(notificationService).sendMessage(123L, anyString());
        verify(repository).save(alert);
    }

    private FlightOffer offer(int price) {
        return new FlightOffer(
                price,
                "SU",
                "SVO",
                "IST",
                0,
                OffsetDateTime.parse("2027-10-27T12:00:00+03:00")
        );
    }
}
