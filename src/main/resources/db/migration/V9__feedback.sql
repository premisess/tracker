-- Feedback, feature recommendations and problem reports sent from the app, with the team's reply.
CREATE TABLE `feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `kind` enum('FEEDBACK','RECOMMENDATION','PROBLEM') NOT NULL,
  `rating` int DEFAULT NULL,
  `message` varchar(2000) NOT NULL,
  `status` enum('NEW','REVIEWED','PLANNED','DONE') NOT NULL,
  `reply` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_user` (`user_id`, `created_at`),
  KEY `idx_feedback_status` (`status`, `created_at`),
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
