package com.trizen.photoshare.service.storage;

import com.trizen.photoshare.config.AppProperties;
import com.trizen.photoshare.exception.NotFoundException;
import com.trizen.photoshare.exception.StorageException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/**
 * Default implementation: files on disk, served back through a signed URL.
 * Good enough for local development and a single node deployment with a volume.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;
    private final AppProperties properties;
    private final UrlSigner urlSigner;

    public LocalStorageService(AppProperties properties, UrlSigner urlSigner) {
        this.properties = properties;
        this.urlSigner = urlSigner;
        this.root = Path.of(properties.getStorage().getLocalDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new StorageException("Cannot create the upload directory at " + root);
        }
    }

    @Override
    public String store(String key, InputStream content, long contentLength, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException ex) {
            throw new StorageException("Could not save " + key);
        }
    }

    @Override
    public String presignedUrl(String key) {
        long expiresAt = Instant.now()
                .plusSeconds(properties.getStorage().getSignedUrlMinutes() * 60)
                .getEpochSecond();
        String signature = urlSigner.sign(key, expiresAt);
        return properties.getPublicBaseUrl() + "/api/files/" + key
                + "?expires=" + expiresAt + "&signature=" + signature;
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            throw new StorageException("Could not delete " + key);
        }
    }

    @Override
    public StoredContent load(String key) {
        Path target = resolve(key);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new NotFoundException("That file is no longer available");
        }
        try {
            String contentType = Files.probeContentType(target);
            return new StoredContent(new FileSystemResource(target),
                    contentType == null ? "application/octet-stream" : contentType,
                    Files.size(target));
        } catch (IOException ex) {
            throw new StorageException("Could not read " + key);
        }
    }

    @Override
    public String type() {
        return "local";
    }

    /** Blocks path traversal: the resolved file must stay inside the storage root. */
    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new StorageException("Invalid storage key");
        }
        return target;
    }
}
