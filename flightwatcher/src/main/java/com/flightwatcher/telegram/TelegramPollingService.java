package com.flightwatcher.telegram;

import com.flightwatcher.telegram.dto.TelegramCallbackQuery;
import com.flightwatcher.telegram.dto.TelegramUpdate;
import com.flightwatcher.telegram.dto.TelegramUpdatesResponse;
import com.flightwatcher.telegram.flow.TelegramAddSubscriptionService;
import com.flightwatcher.telegram.flow.TelegramDeleteSubscriptionService;
import com.flightwatcher.telegram.flow.TelegramEditSubscriptionService;
import com.flightwatcher.telegram.flow.TelegramGetSubscriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;

@Component
public class TelegramPollingService {

    private final RestClient restClient;
    private final String botToken;
    private final TelegramNotificationService notificationService;
    private final TelegramCommandService commandService;
    private final TelegramAddSubscriptionService addSubscriptionService;
    private final TelegramDeleteSubscriptionService deleteSubscriptionService;
    private final TelegramGetSubscriptionService getSubscriptionService;
    private final TelegramEditSubscriptionService editSubscriptionService;

    private long offset = 0;

    public TelegramPollingService(
            RestClient.Builder builder,
            @Value("${telegram.bot-token}") String botToken,
            TelegramNotificationService notificationService,
            TelegramCommandService commandService,
            TelegramAddSubscriptionService addSubscriptionService,
            TelegramDeleteSubscriptionService deleteSubscriptionService,
            TelegramGetSubscriptionService getSubscriptionService,
            TelegramEditSubscriptionService editSubscriptionService
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
        this.notificationService = notificationService;
        this.commandService = commandService;
        this.addSubscriptionService = addSubscriptionService;
        this.deleteSubscriptionService = deleteSubscriptionService;
        this.getSubscriptionService = getSubscriptionService;
        this.editSubscriptionService = editSubscriptionService;
    }

    @Scheduled(fixedDelay = 3000)
    public void poll() {
        try {
            TelegramUpdatesResponse response = restClient.get()
                    .uri(botMethodUri("getUpdates?offset=" + offset))
                    .retrieve()
                    .body(TelegramUpdatesResponse.class);

            if (response == null || !response.ok() || response.result() == null) {
                return;
            }

            for (TelegramUpdate update : response.result()) {
                try {
                    if (update.callbackQuery() != null) {
                        handleCallback(update.callbackQuery());
                    } else if (update.message() != null
                            && update.message().chat() != null
                            && update.message().text() != null) {
                        handleMessage(
                                update.message().chat().id(),
                                update.message().text()
                        );
                    }

                    offset = update.updateId() + 1;

                } catch (Exception e) {
                    System.err.println(
                            "Ошибка обработки Telegram update "
                                    + update.updateId() + ": "
                                    + e.getClass().getSimpleName()
                    );
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "Ошибка Telegram polling: " + e.getClass().getSimpleName()
            );
        }
    }

    private void handleCallback(TelegramCallbackQuery callbackQuery) {
        if (callbackQuery.message() == null || callbackQuery.message().chat() == null) {
            notificationService.answerCallbackQuery(callbackQuery.id());
            return;
        }

        notificationService.answerCallbackQuery(callbackQuery.id());

        Long chatId = callbackQuery.message().chat().id();
        String data = callbackQuery.data();

        if (data != null && data.startsWith("fw:")) {
            addSubscriptionService.handleCallback(chatId, data);
        } else if (data != null
                && data.startsWith(TelegramDeleteSubscriptionService.CALLBACK_PREFIX)) {
            deleteSubscriptionService.handleCallback(chatId, data);
        } else if (data != null
                && data.startsWith(TelegramGetSubscriptionService.CALLBACK_PREFIX)) {
            getSubscriptionService.handleCallback(chatId, data);
        } else if (data != null
                && data.startsWith(TelegramEditSubscriptionService.CALLBACK_PREFIX)) {
            addSubscriptionService.cancel(chatId);
            editSubscriptionService.handleCallback(chatId, data);
        }
    }

    private void handleMessage(Long chatId, String text) {
        String trimmedText = text.trim();
        String command = trimmedText.isEmpty()
                ? ""
                : trimmedText.split("\\s+")[0].split("@")[0];

        if (command.equals("/add")) {
            editSubscriptionService.cancel(chatId);
            addSubscriptionService.start(chatId);
            return;
        }

        if (command.equals("/cancel")) {
            boolean cancelled = addSubscriptionService.cancel(chatId)
                    | editSubscriptionService.cancel(chatId);
            String answer = cancelled
                    ? "Действие отменено."
                    : "Сейчас нет незавершённого действия.";
            notificationService.sendMessage(chatId, answer);
            return;
        }

        if (command.equals("/delete")) {
            deleteSubscriptionService.start(chatId);
            return;
        }

        if (command.equals("/get")) {
            getSubscriptionService.start(chatId);
            return;
        }

        if (trimmedText.startsWith("/")) {
            String answer = commandService.handleCommand(chatId, trimmedText);
            notificationService.sendMessage(chatId, answer);
            return;
        }

        if (!editSubscriptionService.handleText(chatId, trimmedText)
                && !addSubscriptionService.handleText(chatId, trimmedText)) {
            notificationService.sendMessage(
                    chatId,
                    "Сначала выберите команду в меню 👇"
            );
        }
    }

    private URI botMethodUri(String methodAndQuery) {
        return URI.create(
                "https://api.telegram.org/bot" + botToken + "/" + methodAndQuery
        );
    }
}
