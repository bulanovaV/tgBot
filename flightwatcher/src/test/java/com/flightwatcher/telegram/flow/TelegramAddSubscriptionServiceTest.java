package com.flightwatcher.telegram.flow;

import com.flightwatcher.airport.AirportCatalogService;
import com.flightwatcher.airport.AirportLookupClient;
import com.flightwatcher.flight.FlightPriceService;
import com.flightwatcher.telegram.TelegramNotificationService;
import com.flightwatcher.flight.dto.FlightOffer;
import com.flightwatcher.subscription.PriceAlert;
import com.flightwatcher.subscription.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramAddSubscriptionServiceTest {

    private static final Long CHAT_ID = 123L;

    private PriceAlertRepository repository;
    private TelegramNotificationService notificationService;
    private FlightPriceService flightPriceService;
    private TelegramAddSubscriptionService service;

    @BeforeEach
    void setUp() {
        repository = mock(PriceAlertRepository.class);
        notificationService = mock(TelegramNotificationService.class);
        flightPriceService = mock(FlightPriceService.class);
        service = new TelegramAddSubscriptionService(
                new AirportCatalogService(),
                repository,
                notificationService,
                flightPriceService,
                mock(AirportLookupClient.class)
        );
    }

    @Test
    void shouldCreateAlertThroughButtonFlow() {
        when(repository.save(any(PriceAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(flightPriceService.findCheapestFlight(any(PriceAlert.class)))
                .thenReturn(Optional.of(new FlightOffer(
                        12000,
                        "SU",
                        "SVO",
                        "IST",
                        0,
                        OffsetDateTime.parse("2099-12-31T12:00:00+03:00")
                )));

        service.start(CHAT_ID);
        service.handleCallback(CHAT_ID, "fw:oc:RU");
        service.handleCallback(CHAT_ID, "fw:oa:SVO");
        service.handleCallback(CHAT_ID, "fw:dc:TR");
        service.handleCallback(CHAT_ID, "fw:da:IST");
        service.handleText(CHAT_ID, "31.12.2099");
        service.handleText(CHAT_ID, "15 000");

        ArgumentCaptor<PriceAlert> alertCaptor =
                ArgumentCaptor.forClass(PriceAlert.class);
        verify(repository).save(alertCaptor.capture());

        PriceAlert savedAlert = alertCaptor.getValue();
        assertEquals("SVO", savedAlert.getOrigin());
        assertEquals("IST", savedAlert.getDestination());
        assertEquals(LocalDate.of(2099, 12, 31), savedAlert.getDepartureDate());
        assertEquals(15000, savedAlert.getMaxPrice());
        assertEquals(CHAT_ID, savedAlert.getTelegramChatId());
        assertEquals(12000, savedAlert.getLastCheckedPrice());
        assertFalse(service.hasActiveDraft(CHAT_ID));
    }

    @Test
    void shouldStartWithCountryKeyboard() {
        service.start(CHAT_ID);

        verify(notificationService).sendInlineKeyboard(
                eq(CHAT_ID),
                contains("страну вылета"),
                anyList()
        );
        assertTrue(service.hasActiveDraft(CHAT_ID));
    }
}
