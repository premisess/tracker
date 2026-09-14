-- Baseline: the schema Hibernate's ddl-auto=update had built before migrations existed.
-- An existing database is adopted at this version without running it (baseline-on-migrate);
-- a brand-new database runs it to get the same starting point.

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `reset_token` varchar(255) DEFAULT NULL,
  `reset_token_expiry` datetime(6) DEFAULT NULL,
  `role` enum('ADMIN','USER') DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `age` int DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `height` double DEFAULT NULL,
  `profile_pic` varchar(255) DEFAULT NULL,
  `weight` double DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4ixsj6aqve5pxrbw2u0oyk8bb` (`user_id`),
  CONSTRAINT `FK410q61iev7klncmpqfuo85ivh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `workouts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `calories_burned` int DEFAULT NULL,
  `date` date DEFAULT NULL,
  `duration` int DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpf8ql3wbw2drijbk1ugfvki3d` (`user_id`),
  CONSTRAINT `FKpf8ql3wbw2drijbk1ugfvki3d` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `goals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `auto_track` bit(1) DEFAULT NULL,
  `current_progress` double DEFAULT NULL,
  `deadline` date DEFAULT NULL,
  `goal_type` enum('BUILD_STRENGTH','GAIN_WEIGHT','LOSE_WEIGHT','RUN_MORE','STAY_ACTIVE') DEFAULT NULL,
  `status` enum('COMPLETED','FAILED','IN_PROGRESS') DEFAULT NULL,
  `target_value` double DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `unit` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKb1mp6ulyqkpcw6bc1a2mr7v1g` (`user_id`),
  CONSTRAINT `FKb1mp6ulyqkpcw6bc1a2mr7v1g` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bmi_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bmi_value` double DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `height` double DEFAULT NULL,
  `weight` double DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsacomn30bmey63jrvgjh1ukt3` (`user_id`),
  CONSTRAINT `FKsacomn30bmey63jrvgjh1ukt3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `water_intake` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount_ml` int NOT NULL,
  `date` date NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKi5a7dmp0vrbmycc9sj91dxjs2` (`user_id`),
  CONSTRAINT `FKi5a7dmp0vrbmycc9sj91dxjs2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
