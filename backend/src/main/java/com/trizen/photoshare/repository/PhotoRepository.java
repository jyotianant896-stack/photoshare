package com.trizen.photoshare.repository;

import com.trizen.photoshare.entity.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    @Query(value = "select p from Photo p join fetch p.uploadedBy where p.event.id = :eventId",
            countQuery = "select count(p) from Photo p where p.event.id = :eventId")
    Page<Photo> findByEvent(@Param("eventId") Long eventId, Pageable pageable);

    @Query(value = """
            select p from Photo p join fetch p.uploadedBy
            where p.event.id = :eventId and p.uploadedBy.id = :userId
            """,
            countQuery = "select count(p) from Photo p where p.event.id = :eventId and p.uploadedBy.id = :userId")
    Page<Photo> findByEventAndUploader(@Param("eventId") Long eventId,
                                       @Param("userId") Long userId,
                                       Pageable pageable);

    List<Photo> findByIdInAndEventId(List<Long> ids, Long eventId);

    long countByEventId(Long eventId);
}
