package com.flightwatcher.telegram.flow;

import com.flightwatcher.airport.Airport;
import com.flightwatcher.airport.AirportCatalogService;
import com.flightwatcher.airport.CountryAirports;
import com.flightwatcher.airport.AirportLookupClient;
import com.flightwatcher.airport.dto.AirportLookupResult;
import com.flightwatcher.flight.FlightPriceService;
import com.flightwatcher.telegram.TelegramNotificationService;
import com.flightwatcher.telegram.dto.TelegramInlineButton;
import com.flightwatcher.subscription.PriceAlert;
import com.flightwatcher.subscription.PriceAlertRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelegramAddSubscriptionService {

    private static final String CALLBACK_PREFIX = "fw:";
    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private final AirportCatalogService airportCatalog;
    private final PriceAlertRepository alertRepository;
    private final TelegramNotificationService notificationService;
    private final FlightPriceService flightPriceService;
    private final AirportLookupClient airportLookupClient;
    private final Map<Long, AlertDraft> drafts = new ConcurrentHashMap<>();

    public TelegramAddSubscriptionService(
            AirportCatalogService airportCatalog,
            PriceAlertRepository alertRepository,
            TelegramNotificationService notificationService,
            FlightPriceService flightPriceService,
            AirportLookupClient airportLookupClient
    ) {
        this.airportCatalog = airportCatalog;
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
        this.flightPriceService = flightPriceService;
        this.airportLookupClient = airportLookupClient;
    }

    public void start(Long chatId) {
        drafts.put(chatId, new AlertDraft());
        sendCountryMenu(chatId, "Выберите страну вылета:", "oc");
    }

    public boolean cancel(Long chatId) {
        return drafts.remove(chatId) != null;
    }

    public boolean hasActiveDraft(Long chatId) {
        return drafts.containsKey(chatId);
    }

    public boolean handleCallback(Long chatId, String data) {
        if (data == null || !data.startsWith(CALLBACK_PREFIX)) {
            return false;
        }

        AlertDraft draft = drafts.get(chatId);

        if (draft == null) {
            notificationService.sendMessage(
                    chatId,
                    "Сценарий создания подписки устарел. Нажмите /add и начните заново."
            );
            return true;
        }

        String[] parts = data.split(":", 3);

        if (parts.length != 3) {
            sendRestartMessage(chatId);
            return true;
        }

        synchronized (draft) {
            switch (parts[1]) {
                case "oc" -> selectOriginCountry(chatId, draft, parts[2]);
                case "oa" -> selectOriginAirport(chatId, draft, parts[2]);
                case "dc" -> selectDestinationCountry(chatId, draft, parts[2]);
                case "da" -> selectDestinationAirport(chatId, draft, parts[2]);
                case "custom" -> selectCustomAirport(chatId, draft, parts[2]);
                default -> sendRestartMessage(chatId);
            }
        }

        return true;
    }

    public boolean handleText(Long chatId, String text) {
        AlertDraft draft = drafts.get(chatId);

        if (draft == null) {
            return false;
        }

        synchronized (draft) {
            if (draft.step == Step.WAITING_DATE) {
                handleDate(chatId, draft, text);
                return true;
            }

            if (draft.step == Step.WAITING_BUDGET) {
                handleBudget(chatId, draft, text);
                return true;
            }

            if (draft.step == Step.WAITING_CUSTOM_ORIGIN
                    || draft.step == Step.WAITING_CUSTOM_DESTINATION) {
                handleCustomAirport(chatId, draft, text);
                return true;
            }
        }

        notificationService.sendMessage(
                chatId,
                "Пожалуйста, сделайте выбор кнопкой или отправьте /cancel."
        );
        return true;
    }

    private void selectOriginCountry(Long chatId, AlertDraft draft, String countryCode) {
        if (draft.step != Step.ORIGIN_COUNTRY) {
            sendRestartMessage(chatId);
            return;
        }

        CountryAirports country = airportCatalog.findCountry(countryCode).orElse(null);

        if (country == null) {
            sendRestartMessage(chatId);
            return;
        }

        draft.originCountryCode = country.code();
        draft.step = Step.ORIGIN_AIRPORT;
        sendAirportMenu(chatId, country, "Выберите аэропорт вылета:", "oa");
    }

    private void selectOriginAirport(Long chatId, AlertDraft draft, String airportCode) {
        if (draft.step != Step.ORIGIN_AIRPORT) {
            sendRestartMessage(chatId);
            return;
        }

        Airport airport = airportCatalog
                .findAirport(draft.originCountryCode, airportCode)
                .orElse(null);

        if (airport == null) {
            sendRestartMessage(chatId);
            return;
        }

        draft.origin = airport.code();
        draft.step = Step.DESTINATION_COUNTRY;
        sendCountryMenu(chatId, "Выберите страну прилёта:", "dc");
    }

    private void selectDestinationCountry(Long chatId, AlertDraft draft, String countryCode) {
        if (draft.step != Step.DESTINATION_COUNTRY) {
            sendRestartMessage(chatId);
            return;
        }

        CountryAirports country = airportCatalog.findCountry(countryCode).orElse(null);

        if (country == null) {
            sendRestartMessage(chatId);
            return;
        }

        draft.destinationCountryCode = country.code();
        draft.step = Step.DESTINATION_AIRPORT;
        sendAirportMenu(chatId, country, "Выберите аэропорт прилёта:", "da");
    }

    private void selectDestinationAirport(Long chatId, AlertDraft draft, String airportCode) {
        if (draft.step != Step.DESTINATION_AIRPORT) {
            sendRestartMessage(chatId);
            return;
        }

        Airport airport = airportCatalog
                .findAirport(draft.destinationCountryCode, airportCode)
                .orElse(null);

        if (airport == null) {
            sendRestartMessage(chatId);
            return;
        }

        if (airport.code().equals(draft.origin)) {
            notificationService.sendMessage(
                    chatId,
                    "Аэропорт прилёта должен отличаться от аэропорта вылета. Выберите другой аэропорт."
            );
            return;
        }

        draft.destination = airport.code();
        requestDepartureDate(chatId, draft);
    }

    private void selectCustomAirport(
            Long chatId,
            AlertDraft draft,
            String direction
    ) {
        if (direction.equals("origin") && draft.step == Step.ORIGIN_COUNTRY) {
            draft.step = Step.WAITING_CUSTOM_ORIGIN;
            notificationService.sendMessage(
                    chatId,
                    "Введите IATA-код аэропорта вылета из трёх латинских букв. Например: JFK"
            );
        } else if (direction.equals("destination")
                && draft.step == Step.DESTINATION_COUNTRY) {
            draft.step = Step.WAITING_CUSTOM_DESTINATION;
            notificationService.sendMessage(
                    chatId,
                    "Введите IATA-код аэропорта прилёта из трёх латинских букв. Например: JFK"
            );
        } else {
            sendRestartMessage(chatId);
        }
    }

    private void handleCustomAirport(Long chatId, AlertDraft draft, String text) {
        String airportCode = text.trim().toUpperCase(Locale.ROOT);

        if (!airportCode.matches("[A-Z]{3}")) {
            notificationService.sendMessage(
                    chatId,
                    "Код должен состоять ровно из трёх латинских букв. Например: JFK"
            );
            return;
        }

        AirportLookupResult airport = airportCatalog.findAirport(airportCode)
                .map(knownAirport -> new AirportLookupResult(
                        "airport",
                        knownAirport.code(),
                        knownAirport.name(),
                        airportCatalog.getCityName(knownAirport.code()),
                        null
                ))
                .orElseGet(() -> findAirportInAviasales(airportCode));

        if (airport == null) {
            notificationService.sendMessage(
                    chatId,
                    "Аэропорт не найден 😔"
            );
            return;
        }

        airportCatalog.registerAirport(
                airport.code(),
                airport.name(),
                airport.cityName()
        );

        String airportDescription = airport.cityName() != null
                ? airport.cityName() + " — " + airport.name()
                : airport.name();

        if (draft.step == Step.WAITING_CUSTOM_ORIGIN) {
            draft.origin = airport.code();
            draft.step = Step.DESTINATION_COUNTRY;
            notificationService.sendMessage(
                    chatId,
                    "✅ Аэропорт вылета найден: " + airportDescription
            );
            sendCountryMenu(chatId, "Выберите страну прилёта:", "dc");
        } else if (draft.step == Step.WAITING_CUSTOM_DESTINATION) {
            if (airport.code().equals(draft.origin)) {
                notificationService.sendMessage(
                        chatId,
                        "Аэропорт прилёта должен отличаться от аэропорта вылета."
                );
                return;
            }
            draft.destination = airport.code();
            notificationService.sendMessage(
                    chatId,
                    "✅ Аэропорт прилёта найден: " + airportDescription
            );
            requestDepartureDate(chatId, draft);
        }
    }

    private AirportLookupResult findAirportInAviasales(String airportCode) {
        try {
            return airportLookupClient.findAirport(airportCode).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void requestDepartureDate(Long chatId, AlertDraft draft) {
        draft.step = Step.WAITING_DATE;
        String exampleDate = LocalDate.now(APP_ZONE)
                .plusMonths(3)
                .format(DATE_FORMATTER);
        notificationService.sendMessage(
                chatId,
                "Введите будущую дату вылета в формате ДД.ММ.ГГГГ. Например: "
                        + exampleDate
        );
    }

    private void handleDate(Long chatId, AlertDraft draft, String text) {
        try {
            LocalDate date = LocalDate.parse(text.trim(), DATE_FORMATTER);

            if (!date.isAfter(LocalDate.now(APP_ZONE))) {
                notificationService.sendMessage(
                        chatId,
                        "Дата вылета должна быть не раньше завтрашнего дня. Попробуйте ещё раз."
                );
                return;
            }

            draft.departureDate = date;
            draft.step = Step.WAITING_BUDGET;

            String currentPriceText;
            try {
                PriceAlert priceRequest = new PriceAlert(
                        draft.origin,
                        draft.destination,
                        1,
                        date
                );

                draft.currentPrice = flightPriceService
                        .findCheapestFlight(priceRequest)
                        .map(offer -> offer.price())
                        .orElse(null);

                currentPriceText = draft.currentPrice == null
                        ? "Дата принята: " + date.format(DATE_FORMATTER)
                                + ".\nАвиакомпании или сервис цен пока не опубликовали тарифы на эту дату."
                        : "Дата принята: " + date.format(DATE_FORMATTER)
                                + ".\nСейчас минимальная найденная цена: "
                                + draft.currentPrice + " ₽.";

            } catch (Exception e) {
                draft.currentPrice = null;
                currentPriceText = "Дата принята: " + date.format(DATE_FORMATTER)
                        + ".\nСервис цен временно не ответил, но подписку всё равно можно создать.";
            }

            notificationService.sendMessage(
                    chatId,
                    currentPriceText
                            + "\n\nВведите максимальную цену билета в рублях. Например: 15000"
            );

        } catch (DateTimeParseException e) {
            notificationService.sendMessage(
                    chatId,
                    "Не удалось распознать дату. Используйте формат ДД.ММ.ГГГГ, например 27.10.2027."
            );
        }
    }

    private void handleBudget(Long chatId, AlertDraft draft, String text) {
        try {
            int maxPrice = Integer.parseInt(text.replace(" ", "").trim());

            if (maxPrice <= 0) {
                throw new NumberFormatException();
            }

            PriceAlert alert = new PriceAlert(
                    draft.origin,
                    draft.destination,
                    maxPrice,
                    draft.departureDate
            );
            alert.setTelegramChatId(chatId);
            alert.setLastCheckedPrice(draft.currentPrice);

            PriceAlert saved = alertRepository.save(alert);
            drafts.remove(chatId, draft);

            String currentPriceText = draft.currentPrice == null
                    ? "пока не найдена"
                    : draft.currentPrice + " ₽";

            notificationService.sendMessage(
                    chatId,
                    """
                            ✅ Подписка создана!

                            ✈️ %s → %s
                            📅 %s
                            🎯 Лимит: %d ₽
                            💸 Текущая минимальная цена: %s
                            """.formatted(
                            airportCatalog.getCityName(saved.getOrigin()),
                            airportCatalog.getCityName(saved.getDestination()),
                            saved.getDepartureDate().format(DATE_FORMATTER),
                            saved.getMaxPrice(),
                            currentPriceText
                    )
            );

        } catch (NumberFormatException e) {
            notificationService.sendMessage(
                    chatId,
                    "Цена должна быть целым положительным числом. Например: 15000"
            );
        }
    }

    private void sendCountryMenu(Long chatId, String message, String action) {
        List<TelegramInlineButton> buttons = airportCatalog.getCountries().stream()
                .map(country -> new TelegramInlineButton(
                        country.name(),
                        CALLBACK_PREFIX + action + ":" + country.code()
                ))
                .toList();

        List<List<TelegramInlineButton>> rows = new ArrayList<>();

        for (int index = 0; index < buttons.size(); index += 2) {
            rows.add(buttons.subList(index, Math.min(index + 2, buttons.size())));
        }

        String direction = action.equals("oc") ? "origin" : "destination";
        rows.add(List.of(new TelegramInlineButton(
                "✍️ Свой аэропорт",
                CALLBACK_PREFIX + "custom:" + direction
        )));

        notificationService.sendInlineKeyboard(chatId, message, rows);
    }

    private void sendAirportMenu(
            Long chatId,
            CountryAirports country,
            String message,
            String action
    ) {
        List<List<TelegramInlineButton>> rows = country.airports().stream()
                .map(airport -> List.of(new TelegramInlineButton(
                        airport.code() + " — " + airport.name(),
                        CALLBACK_PREFIX + action + ":" + airport.code()
                )))
                .toList();

        notificationService.sendInlineKeyboard(chatId, message, rows);
    }

    private void sendRestartMessage(Long chatId) {
        notificationService.sendMessage(
                chatId,
                "Этот выбор больше не актуален. Нажмите /add и начните заново."
        );
    }

    private enum Step {
        ORIGIN_COUNTRY,
        ORIGIN_AIRPORT,
        DESTINATION_COUNTRY,
        DESTINATION_AIRPORT,
        WAITING_DATE,
        WAITING_BUDGET,
        WAITING_CUSTOM_ORIGIN,
        WAITING_CUSTOM_DESTINATION
    }

    private static final class AlertDraft {
        private Step step = Step.ORIGIN_COUNTRY;
        private String originCountryCode;
        private String origin;
        private String destinationCountryCode;
        private String destination;
        private LocalDate departureDate;
        private Integer currentPrice;
    }
}
