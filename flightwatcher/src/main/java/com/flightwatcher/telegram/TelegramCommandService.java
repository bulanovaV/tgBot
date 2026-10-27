package com.flightwatcher.telegram;

import org.springframework.stereotype.Service;

@Service
public class TelegramCommandService {

    public String handleCommand(Long chatId, String text) {
        String command = text.trim().split("\\s+")[0].split("@")[0];

        return switch (command) {
            case "/start", "/help" -> getHelpMessage();
            default -> "❌ Неизвестная команда. Напишите /help, чтобы посмотреть инструкцию.";
        };
    }

    private String getHelpMessage() {
        return """
                ✈️ Привет! Я FlightWatcher.

                Я отслеживаю минимальные цены на авиабилеты и сообщаю, когда они меняются.

                /add — создать подписку
                /get — посмотреть или изменить подписку
                /delete — удалить подписку
                /cancel — отменить текущее действие
                /help — показать эту инструкцию
                """;
    }
}
