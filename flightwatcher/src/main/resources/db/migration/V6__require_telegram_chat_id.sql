DELETE FROM price_alert
WHERE telegram_chat_id IS NULL;

ALTER TABLE price_alert
ALTER COLUMN telegram_chat_id SET NOT NULL;