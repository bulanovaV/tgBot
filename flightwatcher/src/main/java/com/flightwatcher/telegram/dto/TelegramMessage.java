package com.flightwatcher.telegram.dto;

public record TelegramMessage(
        TelegramChat chat,
        String text
) {}
