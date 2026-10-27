package com.flightwatcher.telegram.flow;

import com.flightwatcher.airport.AirportDisplayService;
import com.flightwatcher.telegram.dto.TelegramInlineButton;
import com.flightwatcher.telegram.TelegramNotificationService;
import com.flightwatcher.subscription.PriceAlert;
import com.flightwatcher.subscription.PriceAlertRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramGetSubscriptionService {

    public static final String CALLBACK_PREFIX = "fwg:";
    private static final int PAGE_SIZE = 6;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PriceAlertRepository alertRepository;
    private final TelegramNotificationService notificationService;
    private final AirportDisplayService airportDisplayService;

    public TelegramGetSubscriptionService(
            PriceAlertRepository alertRepository,
            TelegramNotificationService notificationService,
            AirportDisplayService airportDisplayService
    ) {
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
        this.airportDisplayService = airportDisplayService;
    }

    public void start(Long chatId) {
        showPage(chatId, 0);
    }

    public void handleCallback(Long chatId, String data) {
        String[] parts = data.split(":", 3);

        if (parts.length != 3) {
            sendExpiredButtonMessage(chatId);
            return;
        }

        try {
            if (parts[1].equals("p")) {
                showPage(chatId, Integer.parseInt(parts[2]));
            } else if (parts[1].equals("g")) {
                showAlert(chatId, Long.parseLong(parts[2]));
            } else {
                sendExpiredButtonMessage(chatId);
            }
        } catch (NumberFormatException e) {
            sendExpiredButtonMessage(chatId);
        }
    }

    public void showAlert(Long chatId, Long alertId) {
        alertRepository.findByIdAndTelegramChatId(alertId, chatId)
                .ifPresentOrElse(
                        alert -> notificationService.sendInlineKeyboard(
                                chatId,
                                formatAlertDetails(alert),
                                List.of(List.of(
                                        new TelegramInlineButton(
                                                "✏️ Изменить",
                                                TelegramEditSubscriptionService.CALLBACK_PREFIX
                                                        + "start:" + alert.getId()
                                        ),
                                        new TelegramInlineButton(
                                                "🗑 Удалить",
                                                TelegramDeleteSubscriptionService.CALLBACK_PREFIX
                                                        + "d:" + alert.getId()
                                        )
                                ))
                        ),
                        () -> notificationService.sendMessage(
                                chatId,
                                "Подписка не найдена или принадлежит другому пользователю."
                        )
                );
    }

    private void showPage(Long chatId, int requestedPage) {
        List<PriceAlert> alerts =
                alertRepository.findByTelegramChatIdOrderByIdAsc(chatId);

        if (alerts.isEmpty()) {
            notificationService.sendMessage(chatId, "📭 У вас пока нет подписок.");
            return;
        }

        int pageCount = (alerts.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int fromIndex = page * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, alerts.size());
        List<List<TelegramInlineButton>> rows = new ArrayList<>();

        for (PriceAlert alert : alerts.subList(fromIndex, toIndex)) {
            String text = "%s → %s | %s | %d ₽".formatted(
                    airportDisplayService.getCityName(alert.getOrigin()),
                    airportDisplayService.getCityName(alert.getDestination()),
                    alert.getDepartureDate().format(DATE_FORMATTER),
                    alert.getMaxPrice()
            );
            rows.add(List.of(new TelegramInlineButton(
                    text,
                    CALLBACK_PREFIX + "g:" + alert.getId()
            )));
        }

        List<TelegramInlineButton> navigation = new ArrayList<>();
        if (page > 0) {
            navigation.add(new TelegramInlineButton(
                    "← Назад",
                    CALLBACK_PREFIX + "p:" + (page - 1)
            ));
        }
        if (page + 1 < pageCount) {
            navigation.add(new TelegramInlineButton(
                    "Другие подписки →",
                    CALLBACK_PREFIX + "p:" + (page + 1)
            ));
        }
        if (!navigation.isEmpty()) {
            rows.add(navigation);
        }

        notificationService.sendInlineKeyboard(
                chatId,
                "Выберите подписку (страница %d из %d):"
                        .formatted(page + 1, pageCount),
                rows
        );
    }

    private String formatAlertDetails(PriceAlert alert) {
        Integer price = alert.getLastCheckedPrice();
        String priceText = price == null ? "пока не найдена" : price + " ₽";

        String status;
        if (price == null) {
            status = "⏳ Ожидаем первую успешную проверку";
        } else if (price <= alert.getMaxPrice()) {
            status = "✅ Цена в пределах бюджета";
        } else {
            status = "⚠️ Цена выше бюджета";
        }

        return """
                ✈️ %s → %s
                🛫 Аэропорт вылета: %s
                🛬 Аэропорт прилёта: %s
                📅 Дата вылета: %s
                🎯 Бюджет: %d ₽
                💸 Последняя минимальная цена: %s

                %s
                """.formatted(
                airportDisplayService.getCityName(alert.getOrigin()),
                airportDisplayService.getCityName(alert.getDestination()),
                alert.getOrigin(),
                alert.getDestination(),
                alert.getDepartureDate().format(DATE_FORMATTER),
                alert.getMaxPrice(),
                priceText,
                status
        );
    }

    private void sendExpiredButtonMessage(Long chatId) {
        notificationService.sendMessage(
                chatId,
                "Кнопка устарела. Нажмите /get ещё раз."
        );
    }
}
