-- Structured strength logging: a workout holds exercises in order, each exercise holds its sets.
-- Deleting a workout removes its exercises and sets with it.

CREATE TABLE `workout_exercises` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `workout_id` bigint NOT NULL,
  `exercise_id` bigint NOT NULL,
  `sort_order` int NOT NULL,
  `notes` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_workout_exercises_workout` (`workout_id`),
  KEY `idx_workout_exercises_exercise` (`exercise_id`),
  CONSTRAINT `fk_workout_exercises_workout` FOREIGN KEY (`workout_id`) REFERENCES `workouts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_workout_exercises_exercise` FOREIGN KEY (`exercise_id`) REFERENCES `exercises` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `exercise_sets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `workout_exercise_id` bigint NOT NULL,
  `set_number` int NOT NULL,
  `reps` int DEFAULT NULL,
  `weight_kg` double DEFAULT NULL,
  `duration_sec` int DEFAULT NULL,
  `rpe` double DEFAULT NULL,
  `warmup` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_exercise_sets_workout_exercise` (`workout_exercise_id`),
  CONSTRAINT `fk_exercise_sets_workout_exercise` FOREIGN KEY (`workout_exercise_id`) REFERENCES `workout_exercises` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
