-- Ultimate extras: workout plans users build themselves, and streak freezes.

-- A plan with an owner is a custom plan only that user can see; built-in plans have no owner.
ALTER TABLE `workout_plans`
  ADD COLUMN `owner_id` bigint DEFAULT NULL,
  ADD COLUMN `created_at` datetime(6) DEFAULT NULL,
  ADD KEY `idx_workout_plans_owner` (`owner_id`),
  ADD CONSTRAINT `fk_workout_plans_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- Custom plans can be deleted, so everything that points at a plan or its sessions goes with it.
ALTER TABLE `plan_enrollments`
  DROP FOREIGN KEY `fk_plan_enrollments_plan`;
ALTER TABLE `plan_enrollments`
  ADD CONSTRAINT `fk_plan_enrollments_plan` FOREIGN KEY (`plan_id`) REFERENCES `workout_plans` (`id`) ON DELETE CASCADE;

ALTER TABLE `plan_session_logs`
  DROP FOREIGN KEY `fk_plan_session_logs_day`;
ALTER TABLE `plan_session_logs`
  ADD CONSTRAINT `fk_plan_session_logs_day` FOREIGN KEY (`plan_day_id`) REFERENCES `plan_days` (`id`) ON DELETE CASCADE;

-- A missed day the user protected, so it doesn't break their streak.
CREATE TABLE `streak_freezes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `freeze_date` date NOT NULL,
  `used_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_streak_freezes_user_date` (`user_id`, `freeze_date`),
  CONSTRAINT `fk_streak_freezes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
