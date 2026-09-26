package com.fitness.tracker.repository;

import com.fitness.tracker.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** Every request or friendship this user is part of, either side. */
    @Query("SELECT f FROM Friendship f JOIN FETCH f.requester JOIN FETCH f.addressee "
            + "WHERE f.requester.id = :userId OR f.addressee.id = :userId")
    List<Friendship> findAllInvolving(@Param("userId") Long userId);

    /** The row linking two people, whichever of them sent the request. */
    @Query("SELECT f FROM Friendship f WHERE (f.requester.id = :a AND f.addressee.id = :b) "
            + "OR (f.requester.id = :b AND f.addressee.id = :a)")
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    long countByAddresseeIdAndStatus(Long addresseeId, Friendship.Status status);
}
