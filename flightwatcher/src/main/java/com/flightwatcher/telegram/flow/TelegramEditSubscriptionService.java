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
public class TelegramEditSubscriptionService {

    public static final String CALLBACK_PREFIX = "fwe:";
    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private final PriceAlertRepository alertRepository;
    private final AirportCatalogService airportCatalog;
    private final TelegramNotificationService notificationService;
    private final TelegramGetSubscriptionService getSubscriptionService;
    private final FlightPriceService flightPriceService;
    private final AirportLookupClient airportLookupClient;
    private final Map<Long, EditDraft> drafts = new ConcurrentHashMap<>();

    public TelegramEditSubscriptionService(
            PriceAlertRepository alertRepository,
            AirportCatalogService airportCatalog,
            TelegramNotificationService notificationService,
            TelegramGetSubscriptionService getSubscriptionService,
            FlightPriceService flightPriceService,
            AirportLookupClient airportLookupClient
    ) {
        this.alertRepository = alertRepository;
        this.airportCatalog = airportCatalog;
        this.notificationService = notificationService;
        this.getSubscriptionService = getSubscriptionService;
        this.flightPriceService = flightPriceService;
        this.airportLookupClient = airportLookupClient;
    }

    public boolean cancel(Long chatId) {
        return drafts.remove(chatId) != null;
    }

    public boolean handleText(Long chatId, String text) {
        EditDraft draft = drafts.get(chatId);
        if (draft == null) {
            return false;
        }

        synchronized (draft) {
            if (draft.step == Step.DATE) {
                updateDate(chatId, draft, text);
                return true;
            }
            if (draft.step == Step.PRICE) {
                updatePrice(chatId, draft, text);
                return true;
            }
            if (draft.step == Step.CUSTOM_AIRPORT) {
                updateCustomAirport(chatId, draft, text);
                return true;
            }
        }

        notificationService.sendMessage(
                chatId,
                "Выберите параметр кнопкой или отправьте /cancel."
        );
        return true;
    }

    public void handleCallback(Long chatId, String data) {
        String[] parts = data.split(":", 3);
        if (parts.length != 3) {
            sendExpiredMessage(chatId);
            return;
        }

        switch (parts[1]) {
            case "start" -> start(chatId, parseId(chatId, parts[2]));
            case "field" -> selectField(chatId, parts[2]);
            case "country" -> selectCountry(chatId, parts[2]);
            case "airport" -> selectAirport(chatId, parts[2]);
            case "custom" -> requestCustomAirport(chatId);
            default -> sendExpiredMessage(chatId);
        }
    }

    private Long parseId(Long chatId, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            sendExpiredMessage(chatId);
            return null;
        }
    }

    private void start(Long chatId, Long alertId) {
        if (alertId == null) {
            return;
        }

        alertRepository.findByIdAndTelegramChatId(alertId, chatId)
                .ifPresentOrElse(alert -> {
                    drafts.put(chatId, new EditDraft(alertId));
                    notificationService.sendInlineKeyboard(
                            chatId,
                            "Что изменить?",
                            List.of(
                                    List.of(
                                            new TelegramInlineButton(
                                                    "🛫 Вылет",
                                                    CALLBACK_PREFIX + "field:origin"
                                            ),
                                            new TelegramInlineButton(
                                                    "🛬 Прилёт",
                                                    CALLBACK_PREFIX + "field:destination"
                                            )
                                    ),
                                    List.of(
                                            new TelegramInlineButton(
                                                    "📅 Дату",
                                                    CALLBACK_PREFIX + "field:date"
                                            ),
                                            new TelegramInlineButton(
                                                    "🎯 Лимит цены",
                                                    CALLBACK_PREFIX + "field:price"
                                            )
                                    )
                            )
                    );
                }, () -> notificationService.sendMessage(chatId, "Подписка не найдена."));
    }

    private void selectField(Long chatId, String fieldValue) {
        EditDraft draft = drafts.get(chatId);
        if (draft == null) {
            sendExpiredMessage(chatId);
            return;
        }

        synchronized (draft) {
            switch (fieldValue) {
                case "origin" -> {
                    draft.field = Field.ORIGIN;
                    draft.step = Step.COUNTRY;
                    sendCountryMenu(chatId, "Выберите новую страну вылета:");
                }
                case "destination" -> {
                    draft.field = Field.DESTINATION;
                    draft.step = Step.COUNTRY;
                    sendCountryMenu(chatId, "Выберите новую страну прилёта:");
                }
                case "date" -> {
                    draft.field = Field.DATE;
                    draft.step = Step.DATE;
                    notificationService.sendMessage(
                            chatId,
                            "Введите новую дату в формате ДД.ММ.ГГГГ."
                    );
                }
                case "price" -> {
                    draft.field = Field.PRICE;
                    draft.step = Step.PRICE;
                    notificationService.sendMessage(
                            chatId,
                            "Введите новый максимальный бюджет в рублях."
                    );
                }
                default -> sendExpiredMessage(chatId);
            }
        }
    }

    private void selectCountry(Long chatId, String countryCode) {
        EditDraft draft = drafts.get(chatId);
        if (draft == null || draft.step != Step.COUNTRY) {
            sendExpiredMessage(chatId);
            return;
        }

        CountryAirports country = airportCatalog.findCountry(countryCode).orElse(null);
        if (country == null) {
            sendExpiredMessage(chatId);
            return;
        }

        draft.countryCode = country.code();
        draft.step = Step.AIRPORT;
        sendAirportMenu(chatId, country);
    }

    private void selectAirport(Long chatId, String airportCode) {
        EditDraft draft = drafts.get(chatId);
        if (draft == null || draft.step != Step.AIRPORT) {
            sendExpiredMessage(chatId);
            return;
        }

        Airport airport = airportCatalog
                .findAirport(draft.countryCode, airportCode)
                .orElse(null);

        if (airport == null) {
            sendExpiredMessage(chatId);
            return;
        }

        applyAirportChange(chatId, draft, airport.code());
    }

    private void requestCustomAirport(Long chatId) {
        EditDraft draft = drafts.get(chatId);
        if (draft == null || draft.step != Step.COUNTRY) {
            sendExpiredMessage(chatId);
            return;
        }

        draft.step = Step.CUSTOM_AIRPORT;
        notificationService.sendMessage(
                chatId,
                "Введите новый IATA-код аэропорта из трёх латинских букв. Например: JFK"
        );
    }

    private void updateCustomAirport(Long chatId, EditDraft draft, String text) {
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

        applyAirportChange(chatId, draft, airport.code());
    }

    private AirportLookupResult findAirportInAviasales(String airportCode) {
        try {
            return airportLookupClient.findAirport(airportCode).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void applyAirportChange(
            Long chatId,
            EditDraft draft,
            String airportCode
    ) {
        PriceAlert alert = findOwnedAlert(chatId, draft);
        if (alert == null) {
            sendExpiredMessage(chatId);
            return;
        }

        if (draft.field == Field.ORIGIN) {
            if (airportCode.equals(alert.getDestination())) {
                notificationService.sendMessage(
                        chatId,
                        "Аэропорт вылета должен отличаться от аэропорта прилёта."
                );
                return;
            }
            alert.setOrigin(airportCode);
        } else if (draft.field == Field.DESTINATION) {
            if (airportCode.equals(alert.getOrigin())) {
                notificationService.sendMessage(
                        chatId,
                        "Аэропорт прилёта должен отличаться от аэропорта вылета."
                );
                return;
            }
            alert.setDestination(airportCode);
        } else {
            sendExpiredMessage(chatId);
            return;
        }

        refreshPrice(alert);
        alertRepository.save(alert);
        finish(chatId, draft);
    }

    private void updateDate(Long chatId, EditDraft draft, String text) {
        try {
            LocalDate date = LocalDate.parse(text.trim(), DATE_FORMATTER);
            if (!date.isAfter(LocalDate.now(APP_ZONE))) {
                notificationService.sendMessage(
                        chatId,
                        "Дата вылета должна быть не раньше завтрашнего дня."
                );
                return;
            }

            PriceAlert alert = findOwnedAlert(chatId, draft);
            if (alert == null) {
                sendExpiredMessage(chatId);
                return;
            }

            alert.setDepartureDate(date);
            refreshPrice(alert);
            alertRepository.save(alert);
            finish(chatId, draft);

        } catch (DateTimeParseException e) {
            notificationService.sendMessage(
                    chatId,
                    "Используйте формат ДД.ММ.ГГГГ, например 27.10.2027."
            );
        }
    }

    private void updatePrice(Long chatId, EditDraft draft, String text) {
        try {
            int maxPrice = Integer.parseInt(text.replace(" ", "").trim());
            if (maxPrice <= 0) {
                throw new NumberFormatException();
            }

            PriceAlert alert = findOwnedAlert(chatId, draft);
            if (alert == null) {
                sendExpiredMessage(chatId);
                return;
            }

            alert.setMaxPrice(maxPrice);
            alertRepository.save(alert);
            finish(chatId, draft);

        } catch (NumberFormatException e) {
            notificationService.sendMessage(
                    chatId,
                    "Цена должна быть целым положительным числом. Например: 15000"
            );
        }
    }

    private void refreshPrice(PriceAlert alert) {
        try {
            alert.setLastCheckedPrice(
                    flightPriceService.findCheapestFlight(alert)
                            .map(offer -> offer.price())
                            .orElse(null)
            );
        } catch (Exception e) {
            alert.setLastCheckedPrice(null);
        }
    }

    private PriceAlert findOwnedAlert(Long chatId, EditDraft draft) {
        return alertRepository.findByIdAndTelegramChatId(draft.alertId, chatId)
                .orElse(null);
    }

    private void finish(Long chatId, EditDraft draft) {
        drafts.remove(chatId, draft);
        notificationService.sendMessage(chatId, "✅ Подписка обновлена.");
        getSubscriptionService.showAlert(chatId, draft.alertId);
    }

    private void sendCountryMenu(Long chatId, String message) {
        List<TelegramInlineButton> buttons = airportCatalog.getCountries().stream()
                .map(country -> new TelegramInlineButton(
                        country.name(),
                        CALLBACK_PREFIX + "country:" + country.code()
                ))
                .toList();
        List<List<TelegramInlineButton>> rows = new ArrayList<>();
        for (int index = 0; index < buttons.size(); index += 2) {
            rows.add(buttons.subList(index, Math.min(index + 2, buttons.size())));
        }
        rows.add(List.of(new TelegramInlineButton(
                "✍️ Свой аэропорт",
                CALLBACK_PREFIX + "custom:airport"
        )));
        notificationService.sendInlineKeyboard(chatId, message, rows);
    }

    private void sendAirportMenu(Long chatId, CountryAirports country) {
        List<List<TelegramInlineButton>> rows = country.airports().stream()
                .map(airport -> List.of(new TelegramInlineButton(
                        airport.code() + " — " + airport.name(),
                        CALLBACK_PREFIX + "airport:" + airport.code()
                )))
                .toList();
        notificationService.sendInlineKeyboard(chatId, "Выберите новый аэропорт:", rows);
    }

    private void sendExpiredMessage(Long chatId) {
        notificationService.sendMessage(
                chatId,
                "Редактирование устарело. Откройте подписку через /get ещё раз."
        );
    }

    private enum Field {
        ORIGIN,
        DESTINATION,
        DATE,
        PRICE
    }

    private enum Step {
        FIELD,
        COUNTRY,
        AIRPORT,
        CUSTOM_AIRPORT,
        DATE,
        PRICE
    }

    private static final class EditDraft {
        private final Long alertId;
        private Field field;
        private Step step = Step.FIELD;
        private String countryCode;

        private EditDraft(Long alertId) {
            this.alertId = alertId;
        }
    }
}
