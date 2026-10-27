package com.flightwatcher.subscription;

import com.flightwatcher.subscription.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByTelegramChatId(Long telegramChatId);

    List<PriceAlert> findByTelegramChatIdOrderByIdAsc(Long telegramChatId);

    Optional<PriceAlert> findByIdAndTelegramChatId(
            Long id,
            Long telegramChatId
    );
}
