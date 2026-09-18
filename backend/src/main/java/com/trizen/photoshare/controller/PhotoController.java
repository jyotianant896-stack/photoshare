package com.trizen.photoshare.controller;

import com.trizen.photoshare.dto.PhotoDtos.PhotoPage;
import com.trizen.photoshare.dto.PhotoDtos.UploadResult;
import com.trizen.photoshare.security.AppUserPrincipal;
import com.trizen.photoshare.service.PhotoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping("/events/{eventId}/photos")
    public PhotoPage list(@AuthenticationPrincipal AppUserPrincipal principal,
                          @PathVariable Long eventId,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "60") int size) {
        return photoService.list(eventId, principal, page, size);
    }

    @PostMapping(value = "/events/{eventId}/photos", consumes = "multipart/form-data")
    public UploadResult upload(@AuthenticationPrincipal AppUserPrincipal principal,
                               @PathVariable Long eventId,
                               @RequestParam("files") MultipartFile[] files) {
        return photoService.upload(eventId, principal, files);
    }

    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal,
                                       @PathVariable Long photoId) {
        photoService.delete(photoId, principal);
        return ResponseEntity.noContent().build();
    }
}
