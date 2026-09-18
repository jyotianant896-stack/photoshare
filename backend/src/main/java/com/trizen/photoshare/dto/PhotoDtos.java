package com.trizen.photoshare.dto;

import java.time.Instant;
import java.util.List;

public final class PhotoDtos {

    private PhotoDtos() {
    }

    public record PhotoDto(
            Long id,
            String filename,
            String contentType,
            long fileSize,
            Long uploadedById,
            String uploadedByName,
            Instant createdAt,
            /** Short lived signed URL for the binary. */
            String url,
            boolean selected) {
    }

    public record PhotoPage(List<PhotoDto> items, int page, int size, long totalItems, int totalPages) {
    }

    public record FailedUpload(String filename, String reason) {
    }

    public record UploadResult(List<PhotoDto> uploaded, List<FailedUpload> failed) {
    }
}
