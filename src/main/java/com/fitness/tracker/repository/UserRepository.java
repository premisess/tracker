package com.fitness.tracker.repository;

import com.fitness.tracker.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByGoogleSubject(String googleSubject);

    Optional<User> findByEmailVerificationTokenHash(String emailVerificationTokenHash);

    /** People matching part of their name, or their exact email. Never the searcher themselves. */
    @Query("SELECT u FROM User u WHERE u.id <> :me AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.email) = LOWER(:q)) ORDER BY u.name")
    List<User> search(@Param("me") Long me, @Param("q") String q, Pageable page);
}
