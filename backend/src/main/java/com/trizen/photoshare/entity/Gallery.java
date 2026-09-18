package com.trizen.photoshare.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The customer facing view of an event: a curated subset of photos,
 * reachable by slug and protected by a PIN (stored only as a BCrypt hash).
 */
@Entity
@Table(name = "galleries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_galleries_slug", columnNames = "slug"),
                @UniqueConstraint(name = "uk_galleries_event", columnNames = "event_id")
        })
public class Gallery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_galleries_event"))
    private Event event;

    @Column(nullable = false, length = 32)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "pin_hash", length = 100)
    private String pinHash;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    /** The curated selection. Ordering is kept stable for the customer. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "gallery_photos",
            joinColumns = @JoinColumn(name = "gallery_id"),
            inverseJoinColumns = @JoinColumn(name = "photo_id"))
    @OrderBy("id ASC")
    private Set<Photo> photos = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Gallery() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public Set<Photo> getPhotos() { return photos; }
    public void setPhotos(Set<Photo> photos) { this.photos = photos; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isAccessible() {
        return published && !isExpired();
    }
}
