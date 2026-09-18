package com.trizen.photoshare.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Assignment of a team member to an event. A user only sees events
 * they own (admin) or are assigned to through this table.
 */
@Entity
@Table(name = "event_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_member",
                columnNames = {"event_id", "user_id"}))
public class EventMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_event_members_event"))
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_event_members_user"))
    private User user;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    void onCreate() {
        this.addedAt = Instant.now();
    }

    public EventMember() {
    }

    public EventMember(Event event, User user) {
        this.event = event;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
}
