package com.trizen.photoshare.controller;

import com.trizen.photoshare.dto.GalleryDtos.GalleryAccessResponse;
import com.trizen.photoshare.dto.GalleryDtos.GalleryTeaser;
import com.trizen.photoshare.dto.GalleryDtos.PinRequest;
import com.trizen.photoshare.dto.GalleryDtos.PublicGalleryDto;
import com.trizen.photoshare.service.GalleryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * The only part of the API a customer touches. No account, no session:
 * slug plus PIN in, short lived gallery token out.
 */
@RestController
@RequestMapping("/api/public/galleries")
public class PublicGalleryController {

    private final GalleryService galleryService;

    public PublicGalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    /** Enough to render the unlock screen, and nothing that is behind the PIN. */
    @GetMapping("/{slug}")
    public GalleryTeaser teaser(@PathVariable String slug) {
        return galleryService.teaser(slug);
    }

    @PostMapping("/{slug}/access")
    public GalleryAccessResponse access(@PathVariable String slug,
                                        @Valid @RequestBody PinRequest request,
                                        HttpServletRequest httpRequest) {
        return galleryService.verifyPin(slug, request.pin(), clientKey(httpRequest));
    }

    @GetMapping("/{slug}/photos")
    public PublicGalleryDto photos(@PathVariable String slug,
                                   @RequestHeader(value = "X-Gallery-Token", required = false) String token) {
        return galleryService.viewWithToken(slug, token);
    }

    /** Identifies the caller for rate limiting, honouring a proxy header when present. */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
