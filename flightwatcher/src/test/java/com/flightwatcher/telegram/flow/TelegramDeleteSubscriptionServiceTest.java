package com.flightwatcher.telegram.flow;

import com.flightwatcher.airport.AirportCatalogService;
import com.flightwatcher.airport.AirportLookupClient;
import com.flightwatcher.airport.AirportDisplayService;
import com.flightwatcher.telegram.TelegramNotificationService;
import com.flightwatcher.telegram.dto.TelegramInlineButton;
import com.flightwatcher.subscription.PriceAlert;
import com.flightwatcher.subscription.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramDeleteSubscriptionServiceTest {

    private static final Long CHAT_ID = 123L;

    private PriceAlertRepository repository;
    private TelegramNotificationService notificationService;
    private TelegramDeleteSubscriptionService service;

    @BeforeEach
    void setUp() {
        repository = mock(PriceAlertRepository.class);
        notificationService = mock(TelegramNotificationService.class);
        service = new TelegramDeleteSubscriptionService(
                repository,
                notificationService,
                new AirportDisplayService(
                        new AirportCatalogService(),
                        mock(AirportLookupClient.class)
                )
        );
    }

    @Test
    void shouldShowSixAlertsAndNextPageButton() {
        when(repository.findByTelegramChatIdOrderByIdAsc(CHAT_ID))
                .thenReturn(createAlerts(7));

        service.start(CHAT_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<List<TelegramInlineButton>>> rowsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(notificationService).sendInlineKeyboard(
                eq(CHAT_ID),
                anyString(),
                rowsCaptor.capture()
        );

        List<List<TelegramInlineButton>> rows = rowsCaptor.getValue();
        assertEquals(7, rows.size());
        assertEquals(6, rows.subList(0, 6).size());
        assertTrue(rows.get(6).getFirst().text().contains("Другие подписки"));
    }

    @Test
    void shouldDeleteOnlyAlertOwnedByChat() {
        PriceAlert alert = createAlert(7L);
        when(repository.findByIdAndTelegramChatId(7L, CHAT_ID))
                .thenReturn(Optional.of(alert));
        when(repository.findByTelegramChatIdOrderByIdAsc(CHAT_ID))
                .thenReturn(List.of());

        service.handleCallback(CHAT_ID, "fwd:d:7");

        verify(repository).delete(alert);
    }

    private List<PriceAlert> createAlerts(int count) {
        List<PriceAlert> alerts = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            alerts.add(createAlert(id));
        }
        return alerts;
    }

    private PriceAlert createAlert(Long id) {
        PriceAlert alert = mock(PriceAlert.class);
        when(alert.getId()).thenReturn(id);
        when(alert.getOrigin()).thenReturn("SVO");
        when(alert.getDestination()).thenReturn("IST");
        when(alert.getDepartureDate()).thenReturn(LocalDate.of(2027, 10, 27));
        when(alert.getMaxPrice()).thenReturn(15000);
        return alert;
    }
}
