package com.flightwatcher.telegram.dto;

import java.util.List;

public record TelegramUpdatesResponse(
        boolean ok,
        List<TelegramUpdate> result
) {}
