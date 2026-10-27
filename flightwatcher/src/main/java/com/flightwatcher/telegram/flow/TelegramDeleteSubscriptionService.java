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
public class TelegramDeleteSubscriptionService {

    public static final String CALLBACK_PREFIX = "fwd:";
    private static final int PAGE_SIZE = 6;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PriceAlertRepository alertRepository;
    private final TelegramNotificationService notificationService;
    private final AirportDisplayService airportDisplayService;

    public TelegramDeleteSubscriptionService(
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
            notificationService.sendMessage(chatId, "Кнопка устарела. Нажмите /delete ещё раз.");
            return;
        }

        try {
            if (parts[1].equals("p")) {
                showPage(chatId, Integer.parseInt(parts[2]));
            } else if (parts[1].equals("d")) {
                deleteAlert(chatId, Long.parseLong(parts[2]));
            } else {
                notificationService.sendMessage(chatId, "Кнопка устарела. Нажмите /delete ещё раз.");
            }
        } catch (NumberFormatException e) {
            notificationService.sendMessage(chatId, "Кнопка устарела. Нажмите /delete ещё раз.");
        }
    }

    private void deleteAlert(Long chatId, Long alertId) {
        alertRepository.findByIdAndTelegramChatId(alertId, chatId)
                .ifPresentOrElse(alert -> {
                    alertRepository.delete(alert);
                    notificationService.sendMessage(
                            chatId,
                            "🗑 Подписка %s → %s на %s удалена."
                                    .formatted(
                                            alert.getOrigin(),
                                            alert.getDestination(),
                                            alert.getDepartureDate().format(DATE_FORMATTER)
                                    )
                    );
                    showPage(chatId, 0);
                }, () -> notificationService.sendMessage(
                        chatId,
                        "Подписка уже удалена или принадлежит другому пользователю."
                ));
    }

    private void showPage(Long chatId, int requestedPage) {
        List<PriceAlert> alerts =
                alertRepository.findByTelegramChatIdOrderByIdAsc(chatId);

        if (alerts.isEmpty()) {
            notificationService.sendMessage(chatId, "📭 У вас нет подписок для удаления.");
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
                    CALLBACK_PREFIX + "d:" + alert.getId()
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
                "Выберите подписку для удаления (страница %d из %d):"
                        .formatted(page + 1, pageCount),
                rows
        );
    }
}
