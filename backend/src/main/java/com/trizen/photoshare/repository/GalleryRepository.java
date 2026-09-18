package com.trizen.photoshare.repository;

import com.trizen.photoshare.entity.Gallery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {

    Optional<Gallery> findByEventId(Long eventId);

    Optional<Gallery> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("select g from Gallery g join fetch g.event e left join fetch g.photos where g.slug = :slug")
    Optional<Gallery> findBySlugWithPhotos(@Param("slug") String slug);
}
