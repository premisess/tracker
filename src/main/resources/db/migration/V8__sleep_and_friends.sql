-- Sleep tracking, friends and messages between friends.
-- Everything belongs to a user and goes with them when the account is deleted.

-- One entry per night, keyed by the date the user woke up.
CREATE TABLE `sleep_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `sleep_date` date NOT NULL,
  `bed_time` datetime(6) NOT NULL,
  `wake_time` datetime(6) NOT NULL,
  `quality` tinyint DEFAULT NULL,
  `notes` varchar(280) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sleep_logs_user_date` (`user_id`, `sleep_date`),
  CONSTRAINT `fk_sleep_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A friend request, which becomes a friendship once accepted. One row per pair of people.
CREATE TABLE `friendships` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `requester_id` bigint NOT NULL,
  `addressee_id` bigint NOT NULL,
  `status` enum('PENDING','ACCEPTED') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `responded_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friendships_pair` (`requester_id`, `addressee_id`),
  KEY `idx_friendships_addressee` (`addressee_id`, `status`),
  CONSTRAINT `fk_friendships_requester` FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friendships_addressee` FOREIGN KEY (`addressee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Messages between friends. CHALLENGE and COLLAB are highlighted alerts ("I challenge you", "let's train together").
CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL,
  `recipient_id` bigint NOT NULL,
  `kind` enum('TEXT','CHALLENGE','COLLAB') NOT NULL,
  `body` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_messages_recipient_unread` (`recipient_id`, `read_at`),
  KEY `idx_messages_pair` (`sender_id`, `recipient_id`, `created_at`),
  CONSTRAINT `fk_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_messages_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
