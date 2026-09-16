-- Google sign-in and email verification, the food diary, structured workout plans, and badges.
-- Every new per-user table cascades on user deletion so removing an account needs no extra cleanup.

ALTER TABLE `users`
  ADD COLUMN `email_verified` bit(1) NOT NULL DEFAULT b'0',
  ADD COLUMN `email_verification_token_hash` varchar(64) DEFAULT NULL,
  ADD COLUMN `email_verification_expires_at` datetime(6) DEFAULT NULL,
  ADD COLUMN `auth_provider` varchar(10) NOT NULL DEFAULT 'LOCAL',
  ADD COLUMN `google_subject` varchar(64) DEFAULT NULL,
  ADD UNIQUE KEY `uk_users_google_subject` (`google_subject`),
  ADD UNIQUE KEY `uk_users_email_verification_token` (`email_verification_token_hash`);

-- Accounts that existed before verification was introduced are trusted as they are.
UPDATE `users` SET `email_verified` = b'1';

-- Nutrition: a shared catalog (owner_id NULL) plus foods each user adds for themselves.
CREATE TABLE `foods` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) NOT NULL,
  `serving_label` varchar(60) NOT NULL,
  `serving_grams` double DEFAULT NULL,
  `calories` double NOT NULL,
  `protein_g` double NOT NULL,
  `carbs_g` double NOT NULL,
  `fat_g` double NOT NULL,
  `fiber_g` double DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_foods_name` (`name`),
  KEY `idx_foods_owner` (`owner_id`),
  CONSTRAINT `fk_foods_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Diary entries copy the food's numbers, so editing or deleting a food never rewrites history.
CREATE TABLE `food_log_entries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `log_date` date NOT NULL,
  `meal` varchar(10) NOT NULL,
  `food_id` bigint DEFAULT NULL,
  `name` varchar(120) NOT NULL,
  `serving_label` varchar(60) NOT NULL,
  `servings` double NOT NULL,
  `calories` double NOT NULL,
  `protein_g` double NOT NULL,
  `carbs_g` double NOT NULL,
  `fat_g` double NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_food_log_user_date` (`user_id`, `log_date`),
  KEY `idx_food_log_food` (`food_id`),
  CONSTRAINT `fk_food_log_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_food_log_food` FOREIGN KEY (`food_id`) REFERENCES `foods` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Workout plans. A day with week_number NULL repeats every week; a day with a week number
-- applies to that week only (used by progressive plans such as the beginner running plan).
CREATE TABLE `workout_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slug` varchar(60) NOT NULL,
  `name` varchar(100) NOT NULL,
  `summary` varchar(255) NOT NULL,
  `description` text,
  `goal` varchar(20) NOT NULL,
  `level` varchar(20) NOT NULL,
  `equipment` varchar(60) NOT NULL,
  `duration_weeks` int NOT NULL,
  `days_per_week` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workout_plans_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `plan_days` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `week_number` int DEFAULT NULL,
  `day_number` int NOT NULL,
  `title` varchar(80) NOT NULL,
  `focus` varchar(160) DEFAULT NULL,
  `activity` varchar(10) NOT NULL,
  `workout_type` varchar(30) NOT NULL,
  `target_minutes` int NOT NULL,
  `target_distance_m` int DEFAULT NULL,
  `instructions` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_plan_days_plan` (`plan_id`, `week_number`, `day_number`),
  CONSTRAINT `fk_plan_days_plan` FOREIGN KEY (`plan_id`) REFERENCES `workout_plans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `plan_day_exercises` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_day_id` bigint NOT NULL,
  `exercise_id` bigint NOT NULL,
  `sort_order` int NOT NULL,
  `sets` int NOT NULL,
  `reps` varchar(20) NOT NULL,
  `rest_sec` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_plan_day_exercises_day` (`plan_day_id`),
  CONSTRAINT `fk_plan_day_exercises_day` FOREIGN KEY (`plan_day_id`) REFERENCES `plan_days` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_day_exercises_exercise` FOREIGN KEY (`exercise_id`) REFERENCES `exercises` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `plan_enrollments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `status` varchar(10) NOT NULL,
  `started_on` date NOT NULL,
  `completed_sessions` int NOT NULL DEFAULT 0,
  `finished_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_plan_enrollments_user` (`user_id`, `status`),
  CONSTRAINT `fk_plan_enrollments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_enrollments_plan` FOREIGN KEY (`plan_id`) REFERENCES `workout_plans` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `plan_session_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enrollment_id` bigint NOT NULL,
  `plan_day_id` bigint NOT NULL,
  `session_number` int NOT NULL,
  `workout_id` bigint DEFAULT NULL,
  `skipped` bit(1) NOT NULL,
  `logged_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_session_logs_session` (`enrollment_id`, `session_number`),
  KEY `idx_plan_session_logs_workout` (`workout_id`),
  CONSTRAINT `fk_plan_session_logs_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `plan_enrollments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_session_logs_day` FOREIGN KEY (`plan_day_id`) REFERENCES `plan_days` (`id`),
  CONSTRAINT `fk_plan_session_logs_workout` FOREIGN KEY (`workout_id`) REFERENCES `workouts` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Badges are defined in code (BadgeDefinition); this only records who earned what, and when.
CREATE TABLE `user_badges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `badge_code` varchar(40) NOT NULL,
  `earned_at` datetime(6) NOT NULL,
  `notified` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_badges_user_code` (`user_id`, `badge_code`),
  CONSTRAINT `fk_user_badges_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
