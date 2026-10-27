package com.flightwatcher.telegram;

import com.flightwatcher.telegram.dto.TelegramInlineButton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelegramNotificationService {

    private final RestClient restClient;
    private final String botToken;

    public TelegramNotificationService(
            RestClient.Builder builder,
            @Value("${telegram.bot-token}") String botToken
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = builder.clone()
                .baseUrl("https://api.telegram.org")
                .requestFactory(requestFactory)
                .build();
        this.botToken = botToken;
    }

    public void sendMessage(Long chatId, String message) {
        restClient.post()
                .uri(botMethodUri("sendMessage"))
                .body(Map.of(
                        "chat_id", chatId,
                        "text", message
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public void sendInlineKeyboard(
            Long chatId,
            String message,
            List<List<TelegramInlineButton>> rows
    ) {
        List<List<Map<String, String>>> inlineKeyboard = rows.stream()
                .map(row -> row.stream()
                        .map(button -> Map.of(
                                "text", button.text(),
                                "callback_data", button.callbackData()
                        ))
                        .toList())
                .toList();

        restClient.post()
                .uri(botMethodUri("sendMessage"))
                .body(Map.of(
                        "chat_id", chatId,
                        "text", message,
                        "reply_markup", Map.of("inline_keyboard", inlineKeyboard)
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public void answerCallbackQuery(String callbackQueryId) {
        restClient.post()
                .uri(botMethodUri("answerCallbackQuery"))
                .body(Map.of("callback_query_id", callbackQueryId))
                .retrieve()
                .toBodilessEntity();
    }

    public void configureBotMenu() {
        List<Map<String, String>> commands = List.of(
                Map.of("command", "start", "description", "Главное меню"),
                Map.of("command", "add", "description", "Создать подписку"),
                Map.of("command", "get", "description", "Посмотреть подписку"),
                Map.of("command", "delete", "description", "Удалить подписку"),
                Map.of("command", "cancel", "description", "Отменить действие"),
                Map.of("command", "help", "description", "Помощь")
        );

        setBotCommands(commands, null);
        setBotCommands(commands, "ru");

        restClient.post()
                .uri(botMethodUri("setChatMenuButton"))
                .body(Map.of(
                        "menu_button", Map.of("type", "commands")
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private void setBotCommands(
            List<Map<String, String>> commands,
            String languageCode
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("commands", commands);
        if (languageCode != null) {
            body.put("language_code", languageCode);
        }

        restClient.post()
                .uri(botMethodUri("setMyCommands"))
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private URI botMethodUri(String method) {
        return URI.create("https://api.telegram.org/bot" + botToken + "/" + method);
    }
}
