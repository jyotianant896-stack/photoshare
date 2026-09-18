package com.trizen.photoshare.controller;

import com.trizen.photoshare.service.storage.LocalStorageService;
import com.trizen.photoshare.service.storage.StoredContent;
import com.trizen.photoshare.service.storage.UrlSigner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Serves photo binaries when storage is local disk. The URL itself is the
 * credential: it carries an expiry and an HMAC signature, so it cannot be
 * forged and stops working on its own. With S3 this controller is not
 * registered at all, because presigned URLs point straight at the bucket.
 */
@RestController
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class FileController {

    private final LocalStorageService storageService;
    private final UrlSigner urlSigner;

    public FileController(LocalStorageService storageService, UrlSigner urlSigner) {
        this.storageService = storageService;
        this.urlSigner = urlSigner;
    }

    @GetMapping("/api/files/{*key}")
    public ResponseEntity<Resource> serve(@PathVariable("key") String key,
                                          @RequestParam long expires,
                                          @RequestParam String signature) {
        String storageKey = key.startsWith("/") ? key.substring(1) : key;
        if (!urlSigner.isValid(storageKey, expires, signature)) {
            return ResponseEntity.status(403).build();
        }
        StoredContent content = storageService.load(storageKey);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.contentLength())
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                .body(content.resource());
    }
}
