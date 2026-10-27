package com.flightwatcher.telegram.dto;

public record TelegramCallbackQuery(
        String id,
        TelegramMessage message,
        String data
) {
}
