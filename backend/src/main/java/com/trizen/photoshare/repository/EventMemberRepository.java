package com.trizen.photoshare.repository;

import com.trizen.photoshare.entity.EventMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventMemberRepository extends JpaRepository<EventMember, Long> {

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    Optional<EventMember> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("select m from EventMember m join fetch m.user where m.event.id = :eventId order by m.addedAt asc")
    List<EventMember> findMembersOf(@Param("eventId") Long eventId);

    long countByEventId(Long eventId);
}
