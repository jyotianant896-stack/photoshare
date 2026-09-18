package com.trizen.photoshare.service;

import com.trizen.photoshare.config.AppProperties;
import com.trizen.photoshare.dto.PhotoDtos.FailedUpload;
import com.trizen.photoshare.dto.PhotoDtos.PhotoDto;
import com.trizen.photoshare.dto.PhotoDtos.PhotoPage;
import com.trizen.photoshare.dto.PhotoDtos.UploadResult;
import com.trizen.photoshare.entity.Event;
import com.trizen.photoshare.entity.Gallery;
import com.trizen.photoshare.entity.Photo;
import com.trizen.photoshare.entity.User;
import com.trizen.photoshare.exception.BadRequestException;
import com.trizen.photoshare.exception.ForbiddenException;
import com.trizen.photoshare.exception.NotFoundException;
import com.trizen.photoshare.repository.GalleryRepository;
import com.trizen.photoshare.repository.PhotoRepository;
import com.trizen.photoshare.repository.UserRepository;
import com.trizen.photoshare.security.AppUserPrincipal;
import com.trizen.photoshare.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PhotoService {

    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

    private final PhotoRepository photoRepository;
    private final GalleryRepository galleryRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final StorageService storageService;
    private final AppProperties properties;

    public PhotoService(PhotoRepository photoRepository,
                        GalleryRepository galleryRepository,
                        UserRepository userRepository,
                        EventService eventService,
                        StorageService storageService,
                        AppProperties properties) {
        this.photoRepository = photoRepository;
        this.galleryRepository = galleryRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.storageService = storageService;
        this.properties = properties;
    }

    /**
     * Uploads are processed one by one: a bad file is reported back in `failed`
     * while the rest of the batch still lands. A photographer dropping 200 files
     * should not lose 199 of them because one was a PDF.
     */
    @Transactional
    public UploadResult upload(Long eventId, AppUserPrincipal principal, MultipartFile[] files) {
        Event event = eventService.requireReadAccess(eventId, principal);
        if (files == null || files.length == 0) {
            throw new BadRequestException("Choose at least one photo to upload");
        }
        if (files.length > properties.getUpload().getMaxFilesPerRequest()) {
            throw new BadRequestException("Upload at most "
                    + properties.getUpload().getMaxFilesPerRequest() + " photos at a time");
        }

        User uploader = userRepository.getReferenceById(principal.getId());
        List<PhotoDto> uploaded = new ArrayList<>();
        List<FailedUpload> failed = new ArrayList<>();

        for (MultipartFile file : files) {
            String name = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
            try {
                validate(file);
                String key = buildKey(eventId, name);
                try (InputStream in = file.getInputStream()) {
                    storageService.store(key, in, file.getSize(), file.getContentType());
                }
                Photo photo = new Photo();
                photo.setEvent(event);
                photo.setUploadedBy(uploader);
                photo.setOriginalFilename(sanitizeName(name));
                photo.setStorageKey(key);
                photo.setContentType(file.getContentType());
                photo.setFileSize(file.getSize());
                photoRepository.save(photo);
                uploaded.add(toDto(photo, principal.getName(), Collections.emptySet()));
            } catch (BadRequestException ex) {
                failed.add(new FailedUpload(name, ex.getMessage()));
            } catch (IOException | RuntimeException ex) {
                log.warn("Upload failed for {} on event {}", name, eventId, ex);
                failed.add(new FailedUpload(name, "Could not be saved, try again"));
            }
        }
        return new UploadResult(uploaded, failed);
    }

    /**
     * Admins see the whole event. Team members only ever see their own uploads,
     * which is also what the requirement asks for.
     */
    @Transactional(readOnly = true)
    public PhotoPage list(Long eventId, AppUserPrincipal principal, int page, int size) {
        Event event = eventService.requireReadAccess(eventId, principal);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Photo> result = eventService.canManage(event, principal)
                ? photoRepository.findByEvent(eventId, pageable)
                : photoRepository.findByEventAndUploader(eventId, principal.getId(), pageable);

        Set<Long> selected = selectedPhotoIds(eventId);
        List<PhotoDto> items = result.getContent().stream()
                .map(photo -> toDto(photo, photo.getUploadedBy().getName(), selected))
                .toList();
        return new PhotoPage(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public void delete(Long photoId, AppUserPrincipal principal) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("Photo not found"));
        Event event = eventService.requireReadAccess(photo.getEvent().getId(), principal);

        boolean uploader = photo.getUploadedBy().getId().equals(principal.getId());
        if (!uploader && !eventService.canManage(event, principal)) {
            throw new ForbiddenException("You can only delete photos you uploaded");
        }

        // Drop it from the curated selection first so the gallery stays consistent.
        galleryRepository.findByEventId(event.getId()).ifPresent(gallery -> {
            if (gallery.getPhotos().removeIf(p -> p.getId().equals(photoId))) {
                galleryRepository.save(gallery);
            }
        });
        photoRepository.delete(photo);
        storageService.delete(photo.getStorageKey());
    }

    public PhotoDto toDto(Photo photo, String uploaderName, Set<Long> selectedIds) {
        return new PhotoDto(
                photo.getId(),
                photo.getOriginalFilename(),
                photo.getContentType(),
                photo.getFileSize() == null ? 0 : photo.getFileSize(),
                photo.getUploadedBy().getId(),
                uploaderName,
                photo.getCreatedAt(),
                storageService.presignedUrl(photo.getStorageKey()),
                selectedIds.contains(photo.getId()));
    }

    private Set<Long> selectedPhotoIds(Long eventId) {
        return galleryRepository.findByEventId(eventId)
                .map(Gallery::getPhotos)
                .map(photos -> photos.stream().map(Photo::getId).collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("The file is empty");
        }
        if (file.getSize() > properties.getUpload().getMaxFileSizeBytes()) {
            throw new BadRequestException("Larger than the "
                    + (properties.getUpload().getMaxFileSizeBytes() / (1024 * 1024)) + " MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || !properties.getUpload().getAllowedContentTypes()
                .contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Only JPEG, PNG, WebP and HEIC images are accepted");
        }
    }

    /** Random key, so the stored name can never collide or leak the original path. */
    private String buildKey(Long eventId, String originalName) {
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > -1 && dot < originalName.length() - 1) {
            extension = originalName.substring(dot + 1).toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]", "");
        }
        String suffix = extension.isEmpty() ? "" : "." + extension;
        return "events/" + eventId + "/" + UUID.randomUUID() + suffix;
    }

    private String sanitizeName(String name) {
        String cleaned = name.replaceAll("[\\r\\n\\t]", " ").trim();
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }
}
