package com.trizen.photoshare.repository;

import com.trizen.photoshare.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Every event the user is allowed to see: the ones they own plus the ones
     * they were assigned to as a team member.
     */
    @Query("""
            select distinct e from Event e
            left join EventMember m on m.event = e
            where e.owner.id = :userId or m.user.id = :userId
            order by e.createdAt desc
            """)
    List<Event> findVisibleTo(@Param("userId") Long userId);
}
