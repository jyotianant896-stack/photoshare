package com.trizen.photoshare.controller;

import com.trizen.photoshare.dto.GalleryDtos.GalleryDto;
import com.trizen.photoshare.dto.GalleryDtos.PublishRequest;
import com.trizen.photoshare.dto.GalleryDtos.PublishResult;
import com.trizen.photoshare.dto.GalleryDtos.UpdateSelectionRequest;
import com.trizen.photoshare.security.AppUserPrincipal;
import com.trizen.photoshare.service.GalleryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Everything here is admin only; ownership is checked again in the service. */
@RestController
@RequestMapping("/api/events/{eventId}/gallery")
@PreAuthorize("hasRole('ADMIN')")
public class GalleryController {

    private final GalleryService galleryService;

    public GalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @GetMapping
    public GalleryDto get(@AuthenticationPrincipal AppUserPrincipal principal,
                          @PathVariable Long eventId) {
        return galleryService.getOrCreate(eventId, principal);
    }

    @PutMapping("/selection")
    public GalleryDto updateSelection(@AuthenticationPrincipal AppUserPrincipal principal,
                                      @PathVariable Long eventId,
                                      @Valid @RequestBody UpdateSelectionRequest request) {
        return galleryService.updateSelection(eventId, principal, request);
    }

    @PostMapping("/publish")
    public PublishResult publish(@AuthenticationPrincipal AppUserPrincipal principal,
                                 @PathVariable Long eventId,
                                 @Valid @RequestBody PublishRequest request) {
        return galleryService.publish(eventId, principal, request);
    }

    @PostMapping("/unpublish")
    public GalleryDto unpublish(@AuthenticationPrincipal AppUserPrincipal principal,
                                @PathVariable Long eventId) {
        return galleryService.unpublish(eventId, principal);
    }
}
