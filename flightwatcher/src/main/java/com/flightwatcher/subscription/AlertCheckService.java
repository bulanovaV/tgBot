package com.flightwatcher.subscription;

import com.flightwatcher.flight.dto.FlightOffer;
import com.flightwatcher.flight.FlightPriceService;
import com.flightwatcher.subscription.PriceAlert;
import com.flightwatcher.subscription.PriceAlertRepository;
import com.flightwatcher.telegram.TelegramNotificationService;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class AlertCheckService {

    private static final int PRICE_CHANGE_EPSILON = 200;

    private final PriceAlertRepository alertRepository;
    private final FlightPriceService flightPriceService;
    private final TelegramNotificationService telegramNotificationService;

    public AlertCheckService(
            PriceAlertRepository alertRepository,
            FlightPriceService flightPriceService,
            TelegramNotificationService telegramNotificationService
    ) {
        this.alertRepository = alertRepository;
        this.flightPriceService = flightPriceService;
        this.telegramNotificationService = telegramNotificationService;
    }

    public void checkAllAlerts() {
        List<PriceAlert> alerts = alertRepository.findAll();

        for (PriceAlert alert : alerts) {
            try {
                Optional<FlightOffer> result =
                        flightPriceService.findCheapestFlight(alert);

                if (result.isEmpty()) {
                    continue;
                }

                FlightOffer offer = result.get();

                int currentPrice = offer.price();
                Integer previousPrice = alert.getLastCheckedPrice();

                if (previousPrice != null
                        && Math.abs((long) currentPrice - previousPrice)
                        <= PRICE_CHANGE_EPSILON) {
                    continue;
                }

                String priceChange;

                if (previousPrice == null) {
                    priceChange = "✈️ Первая проверка цены!";
                } else if (currentPrice > previousPrice) {
                    priceChange = "📈 Цена выросла: "
                            + previousPrice + " → " + currentPrice + " ₽";
                } else {
                    priceChange = "📉 Цена снизилась: "
                            + previousPrice + " → " + currentPrice + " ₽";
                }

                String budgetStatus;

                if (currentPrice <= alert.getMaxPrice()) {
                    budgetStatus = "✅ Билет укладывается в твой бюджет!";
                } else {
                    budgetStatus = "⚠️ Билета в пределах бюджета сейчас нет.\n"
                            + "Минимальная найденная цена: " + currentPrice + " ₽";
                }

                String departureTime = offer.departureAt() != null
                        ? offer.departureAt().format(
                        DateTimeFormatter.ofPattern("HH:mm"))
                        : "не указано";

                String message = """
                        %s

                        %s → %s
                        📅 Дата: %s
                        🕒 Время вылета: %s
                        💰 Минимальная цена: %d ₽
                        🎯 Твой лимит: %d ₽

                        %s
                        """.formatted(
                        priceChange,
                        alert.getOrigin(),
                        alert.getDestination(),
                        alert.getDepartureDate(),
                        departureTime,
                        currentPrice,
                        alert.getMaxPrice(),
                        budgetStatus
                );

                telegramNotificationService.sendMessage(
                        alert.getTelegramChatId(),
                        message
                );

                alert.setLastCheckedPrice(currentPrice);
                alertRepository.save(alert);

            } catch (Exception e) {
                System.err.println(
                        "Ошибка проверки подписки ID=" + alert.getId()
                                + ": " + e.getClass().getSimpleName()
                );
            }
        }
    }
}
