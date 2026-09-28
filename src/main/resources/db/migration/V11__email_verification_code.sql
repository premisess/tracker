-- Sign-up is confirmed with a 6-digit code instead of a link; wrong guesses are counted so a code
-- can't be brute-forced (a new code resets the count).
ALTER TABLE `users`
  ADD COLUMN `email_verification_attempts` int NOT NULL DEFAULT 0;
