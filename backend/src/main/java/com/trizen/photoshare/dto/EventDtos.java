package com.trizen.photoshare.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class EventDtos {

    private EventDtos() {
    }

    public record CreateEventRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 500) String description,
            LocalDate eventDate) {
    }

    public record EventDto(
            Long id,
            String name,
            String description,
            LocalDate eventDate,
            String ownerName,
            long photoCount,
            long memberCount,
            boolean canManage,
            boolean galleryPublished,
            Instant createdAt) {
    }

    /**
     * Adds a team member to an event. If no account exists for the email a new
     * TEAM_MEMBER account is created; a password is generated when none is given
     * and returned once so the admin can hand it over.
     */
    public record AddMemberRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 180) String email,
            @Size(min = 8, max = 72) String password) {
    }

    public record MemberDto(
            Long id,
            String name,
            String email,
            String role,
            Instant addedAt,
            /** Only populated in the response that created the account. */
            String generatedPassword) {
    }
}
