-- GPS-tracked runs, walks, hikes and rides.
-- Summary numbers live on the workout; the raw route and per-km splits hang off it and
-- are deleted with it. Users must opt in before any route is stored.

ALTER TABLE `workouts`
  ADD COLUMN `source` varchar(10) NOT NULL DEFAULT 'MANUAL',
  ADD COLUMN `started_at` datetime(6) DEFAULT NULL,
  ADD COLUMN `distance_meters` int DEFAULT NULL,
  ADD COLUMN `moving_time_sec` int DEFAULT NULL,
  ADD COLUMN `elevation_gain_m` int DEFAULT NULL,
  ADD COLUMN `avg_pace_sec_per_km` int DEFAULT NULL;

ALTER TABLE `users`
  ADD COLUMN `location_consent_at` datetime(6) DEFAULT NULL,
  ADD COLUMN `route_privacy_meters` int NOT NULL DEFAULT 200;

CREATE TABLE `workout_routes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `workout_id` bigint NOT NULL,
  `points_json` longtext NOT NULL,
  `polyline` text NOT NULL,
  `share_polyline` text,
  `privacy_meters` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workout_routes_workout` (`workout_id`),
  CONSTRAINT `fk_workout_routes_workout` FOREIGN KEY (`workout_id`) REFERENCES `workouts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `run_splits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `workout_id` bigint NOT NULL,
  `split_index` int NOT NULL,
  `distance_m` int NOT NULL,
  `duration_sec` int NOT NULL,
  `pace_sec_per_km` int NOT NULL,
  `elevation_gain_m` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_run_splits_workout` (`workout_id`),
  CONSTRAINT `fk_run_splits_workout` FOREIGN KEY (`workout_id`) REFERENCES `workouts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
