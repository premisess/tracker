-- Exercise library, seeded on first startup from data/exercises.json (free-exercise-db, public domain).
-- Muscles are stored comma-separated and instructions newline-separated, matching how workout tags are stored.

CREATE TABLE `exercises` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slug` varchar(100) NOT NULL,
  `name` varchar(120) NOT NULL,
  `category` varchar(40) NOT NULL,
  `equipment` varchar(40) DEFAULT NULL,
  `level` varchar(20) DEFAULT NULL,
  `force_type` varchar(20) DEFAULT NULL,
  `mechanic` varchar(20) DEFAULT NULL,
  `primary_muscles` varchar(255) DEFAULT NULL,
  `secondary_muscles` varchar(255) DEFAULT NULL,
  `instructions` text,
  `image_start` varchar(120) DEFAULT NULL,
  `image_end` varchar(120) DEFAULT NULL,
  `tracking_type` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exercises_slug` (`slug`),
  KEY `idx_exercises_name` (`name`),
  KEY `idx_exercises_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
