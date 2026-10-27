package com.flightwatcher.telegram;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TelegramBotMenuInitializer {

    private final TelegramNotificationService notificationService;
    private final AtomicBoolean configured = new AtomicBoolean(false);

    public TelegramBotMenuInitializer(
            TelegramNotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @Scheduled(initialDelay = 0, fixedDelay = 60000)
    public void configureMenu() {
        if (configured.get()) {
            return;
        }

        try {
            notificationService.configureBotMenu();
            configured.set(true);
            System.out.println("Telegram bot menu configured without /list.");
        } catch (Exception e) {
            System.err.println(
                    "Не удалось настроить меню Telegram, повтор через минуту: "
                            + e.getClass().getSimpleName()
            );
        }
    }
}
