package com.flightwatcher.telegram.dto;

public record TelegramInlineButton(
        String text,
        String callbackData
) {
}
