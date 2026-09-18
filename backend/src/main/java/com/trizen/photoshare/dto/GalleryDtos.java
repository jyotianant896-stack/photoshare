package com.trizen.photoshare.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class GalleryDtos {

    private GalleryDtos() {
    }

    public record UpdateSelectionRequest(
            @NotNull List<Long> photoIds) {
    }

    public record PublishRequest(
            @Size(max = 160) String title,
            @Pattern(regexp = "\\d{4,8}", message = "PIN must be 4 to 8 digits")
            String pin,
            Instant expiresAt) {
    }

    /** Admin view of the gallery, including which photos are currently selected. */
    public record GalleryDto(
            Long id,
            String slug,
            String title,
            boolean published,
            Instant publishedAt,
            Instant expiresAt,
            long viewCount,
            int selectedCount,
            String shareUrl,
            List<Long> selectedPhotoIds) {
    }

    /** Returned once at publish time; the plain PIN is never stored or shown again. */
    public record PublishResult(
            String slug,
            String shareUrl,
            String pin,
            String title,
            int photoCount,
            Instant publishedAt,
            Instant expiresAt) {
    }

    public record PinRequest(
            @NotEmpty @Pattern(regexp = "\\d{4,8}", message = "PIN must be 4 to 8 digits")
            String pin) {
    }

    public record PublicPhotoDto(Long id, String filename, String url) {
    }

    public record PublicGalleryDto(
            String title,
            String eventName,
            LocalDate eventDate,
            int photoCount,
            List<PublicPhotoDto> photos) {
    }

    public record GalleryAccessResponse(
            String accessToken,
            long expiresInSeconds,
            PublicGalleryDto gallery) {
    }

    /** Safe to serve without a PIN: just enough to render the unlock screen. */
    public record GalleryTeaser(String title, String eventName, boolean pinRequired) {
    }
}
