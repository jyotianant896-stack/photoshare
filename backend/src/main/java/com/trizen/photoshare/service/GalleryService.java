package com.trizen.photoshare.service;

import com.trizen.photoshare.config.AppProperties;
import com.trizen.photoshare.dto.GalleryDtos.GalleryAccessResponse;
import com.trizen.photoshare.dto.GalleryDtos.GalleryDto;
import com.trizen.photoshare.dto.GalleryDtos.GalleryTeaser;
import com.trizen.photoshare.dto.GalleryDtos.PublicGalleryDto;
import com.trizen.photoshare.dto.GalleryDtos.PublicPhotoDto;
import com.trizen.photoshare.dto.GalleryDtos.PublishRequest;
import com.trizen.photoshare.dto.GalleryDtos.PublishResult;
import com.trizen.photoshare.dto.GalleryDtos.UpdateSelectionRequest;
import com.trizen.photoshare.entity.Event;
import com.trizen.photoshare.entity.Gallery;
import com.trizen.photoshare.entity.Photo;
import com.trizen.photoshare.exception.BadRequestException;
import com.trizen.photoshare.exception.ForbiddenException;
import com.trizen.photoshare.exception.NotFoundException;
import com.trizen.photoshare.exception.TooManyAttemptsException;
import com.trizen.photoshare.repository.GalleryRepository;
import com.trizen.photoshare.repository.PhotoRepository;
import com.trizen.photoshare.security.AppUserPrincipal;
import com.trizen.photoshare.security.JwtService;
import com.trizen.photoshare.service.storage.StorageService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GalleryService {

    private static final String SLUG_ALPHABET = "abcdefghijkmnopqrstuvwxyz23456789";
    private static final int SLUG_LENGTH = 10;

    private final GalleryRepository galleryRepository;
    private final PhotoRepository photoRepository;
    private final EventService eventService;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PinAttemptService pinAttemptService;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();

    public GalleryService(GalleryRepository galleryRepository,
                          PhotoRepository photoRepository,
                          EventService eventService,
                          StorageService storageService,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          PinAttemptService pinAttemptService,
                          AppProperties properties) {
        this.galleryRepository = galleryRepository;
        this.photoRepository = photoRepository;
        this.eventService = eventService;
        this.storageService = storageService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.pinAttemptService = pinAttemptService;
        this.properties = properties;
    }

    // ---------------------------------------------------------------- admin side

    @Transactional
    public GalleryDto getOrCreate(Long eventId, AppUserPrincipal principal) {
        Event event = eventService.requireManageAccess(eventId, principal);
        Gallery gallery = galleryRepository.findByEventId(eventId)
                .orElseGet(() -> createDraft(event));
        return toDto(gallery);
    }

    /** Replaces the selection wholesale, which keeps the UI simple and idempotent. */
    @Transactional
    public GalleryDto updateSelection(Long eventId, AppUserPrincipal principal,
                                      UpdateSelectionRequest request) {
        Event event = eventService.requireManageAccess(eventId, principal);
        Gallery gallery = galleryRepository.findByEventId(eventId)
                .orElseGet(() -> createDraft(event));

        List<Long> ids = request.photoIds().stream().filter(java.util.Objects::nonNull).distinct().toList();
        Set<Photo> photos = new LinkedHashSet<>();
        if (!ids.isEmpty()) {
            // Scoped to the event, so an id from another event can never be attached.
            List<Photo> found = photoRepository.findByIdInAndEventId(ids, eventId);
            if (found.size() != ids.size()) {
                throw new BadRequestException("Some of those photos do not belong to this event");
            }
            photos.addAll(found);
        }
        gallery.getPhotos().clear();
        gallery.getPhotos().addAll(photos);
        galleryRepository.save(gallery);
        return toDto(gallery);
    }

    @Transactional
    public PublishResult publish(Long eventId, AppUserPrincipal principal, PublishRequest request) {
        Event event = eventService.requireManageAccess(eventId, principal);
        Gallery gallery = galleryRepository.findByEventId(eventId)
                .orElseThrow(() -> new BadRequestException("Select some photos before publishing"));

        if (gallery.getPhotos().isEmpty()) {
            throw new BadRequestException("Select at least one photo before publishing");
        }
        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("The expiry date has already passed");
        }

        if (request.title() != null && !request.title().isBlank()) {
            gallery.setTitle(request.title().trim());
        }
        String pin = (request.pin() == null || request.pin().isBlank())
                ? generatePin()
                : request.pin();

        gallery.setPinHash(passwordEncoder.encode(pin));
        gallery.setPublished(true);
        gallery.setPublishedAt(Instant.now());
        gallery.setExpiresAt(request.expiresAt());
        galleryRepository.save(gallery);

        return new PublishResult(gallery.getSlug(), shareUrl(gallery.getSlug()), pin,
                gallery.getTitle(), gallery.getPhotos().size(),
                gallery.getPublishedAt(), gallery.getExpiresAt());
    }

    @Transactional
    public GalleryDto unpublish(Long eventId, AppUserPrincipal principal) {
        eventService.requireManageAccess(eventId, principal);
        Gallery gallery = galleryRepository.findByEventId(eventId)
                .orElseThrow(() -> new NotFoundException("This event has no gallery yet"));
        gallery.setPublished(false);
        galleryRepository.save(gallery);
        return toDto(gallery);
    }

    // --------------------------------------------------------------- customer side

    @Transactional(readOnly = true)
    public GalleryTeaser teaser(String slug) {
        Gallery gallery = galleryRepository.findBySlug(slug)
                .filter(Gallery::isAccessible)
                .orElseThrow(() -> new NotFoundException("This gallery link is not available"));
        return new GalleryTeaser(gallery.getTitle(), gallery.getEvent().getName(), true);
    }

    /**
     * Exchanges a correct PIN for a short lived, gallery scoped token. The token is
     * the only thing that unlocks the photo list, and it works for this slug only.
     */
    @Transactional
    public GalleryAccessResponse verifyPin(String slug, String pin, String client) {
        if (pinAttemptService.isLocked(slug, client)) {
            throw new TooManyAttemptsException("Too many incorrect PINs. Try again in "
                    + Math.max(1, pinAttemptService.secondsUntilUnlock(slug, client) / 60) + " minutes");
        }
        Gallery gallery = galleryRepository.findBySlugWithPhotos(slug)
                .filter(Gallery::isAccessible)
                .orElseThrow(() -> new NotFoundException("This gallery link is not available"));

        if (gallery.getPinHash() == null || !passwordEncoder.matches(pin, gallery.getPinHash())) {
            pinAttemptService.recordFailure(slug, client);
            int left = pinAttemptService.remainingAttempts(slug, client);
            throw new ForbiddenException(left > 0
                    ? "That PIN is not correct. " + left + " attempts left"
                    : "That PIN is not correct");
        }

        pinAttemptService.recordSuccess(slug, client);
        gallery.setViewCount(gallery.getViewCount() + 1);
        galleryRepository.save(gallery);

        return new GalleryAccessResponse(jwtService.issueGalleryToken(slug),
                jwtService.galleryTokenSeconds(), toPublicDto(gallery));
    }

    /** Re-reads the gallery for a customer who already holds a valid token. */
    @Transactional(readOnly = true)
    public PublicGalleryDto viewWithToken(String slug, String token) {
        Claims claims = token == null ? null : jwtService.parse(token, JwtService.TYPE_GALLERY);
        if (claims == null || !slug.equals(claims.getSubject())) {
            throw new ForbiddenException("Enter the gallery PIN to continue");
        }
        Gallery gallery = galleryRepository.findBySlugWithPhotos(slug)
                .filter(Gallery::isAccessible)
                .orElseThrow(() -> new NotFoundException("This gallery link is not available"));
        return toPublicDto(gallery);
    }

    // -------------------------------------------------------------------- helpers

    private Gallery createDraft(Event event) {
        Gallery gallery = new Gallery();
        gallery.setEvent(event);
        gallery.setSlug(generateSlug());
        gallery.setTitle(event.getName());
        gallery.setPublished(false);
        return galleryRepository.save(gallery);
    }

    private PublicGalleryDto toPublicDto(Gallery gallery) {
        List<PublicPhotoDto> photos = gallery.getPhotos().stream()
                .sorted(java.util.Comparator.comparing(Photo::getId))
                .map(photo -> new PublicPhotoDto(photo.getId(), photo.getOriginalFilename(),
                        storageService.presignedUrl(photo.getStorageKey())))
                .toList();
        return new PublicGalleryDto(gallery.getTitle(), gallery.getEvent().getName(),
                gallery.getEvent().getEventDate(), photos.size(), photos);
    }

    private GalleryDto toDto(Gallery gallery) {
        List<Long> ids = gallery.getPhotos().stream()
                .map(Photo::getId)
                .sorted()
                .collect(Collectors.toList());
        return new GalleryDto(gallery.getId(), gallery.getSlug(), gallery.getTitle(),
                gallery.isPublished(), gallery.getPublishedAt(), gallery.getExpiresAt(),
                gallery.getViewCount(), ids.size(), shareUrl(gallery.getSlug()), ids);
    }

    private String shareUrl(String slug) {
        return properties.getFrontendBaseUrl() + "/g/" + slug;
    }

    private String generateSlug() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder builder = new StringBuilder(SLUG_LENGTH);
            for (int i = 0; i < SLUG_LENGTH; i++) {
                builder.append(SLUG_ALPHABET.charAt(random.nextInt(SLUG_ALPHABET.length())));
            }
            String slug = builder.toString();
            if (!galleryRepository.existsBySlug(slug)) {
                return slug;
            }
        }
        throw new IllegalStateException("Could not generate a unique gallery link");
    }

    private String generatePin() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < properties.getGallery().getPinLength(); i++) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }
}
