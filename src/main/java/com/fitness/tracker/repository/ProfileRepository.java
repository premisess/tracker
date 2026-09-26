package com.fitness.tracker.repository;

import com.fitness.tracker.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);

    List<Profile> findByUserIdIn(Collection<Long> userIds);
}