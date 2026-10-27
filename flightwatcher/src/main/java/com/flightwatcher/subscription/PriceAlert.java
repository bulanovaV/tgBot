package com.flightwatcher.subscription;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "price_alert")
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String origin;
    private String destination;
    private Integer maxPrice;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "last_checked_price")
    private Integer lastCheckedPrice;

    @Column(name = "telegram_chat_id",  nullable = false)
    private Long telegramChatId;

    public Integer getLastCheckedPrice() {
        return lastCheckedPrice;
    }

    public void setLastCheckedPrice(Integer lastCheckedPrice) {
        this.lastCheckedPrice = lastCheckedPrice;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(Long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    //for hibernate
    public PriceAlert() {
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public void setDepartureDate(LocalDate newDate) {
        this.departureDate = newDate;
    }

    public PriceAlert(String origin, String destination, Integer maxPrice, LocalDate departureDate) {
        this.origin = origin;
        this.destination = destination;
        this.maxPrice = maxPrice;
        this.departureDate = departureDate;

    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public Long getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }
}