-- FitTracker Ultimate: paid access until a date, bought with Tanzanian mobile money via ClickPesa.
-- There are no automatic renewals with mobile money; each payment extends ultimate_until.

ALTER TABLE `users`
  ADD COLUMN `ultimate_until` datetime(6) DEFAULT NULL;

CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  -- Our reference sent to ClickPesa: letters and digits only, at most 20 characters.
  `order_reference` varchar(20) NOT NULL,
  `plan` varchar(10) NOT NULL,
  `amount_tzs` int NOT NULL,
  `phone_number` varchar(15) NOT NULL,
  `channel` varchar(40) DEFAULT NULL,
  `status` varchar(10) NOT NULL,
  `provider_transaction_id` varchar(80) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_order_reference` (`order_reference`),
  KEY `idx_payments_user` (`user_id`, `created_at`),
  CONSTRAINT `fk_payments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
