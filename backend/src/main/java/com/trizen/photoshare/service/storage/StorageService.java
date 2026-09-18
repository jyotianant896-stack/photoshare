package com.trizen.photoshare.service.storage;

import java.io.InputStream;

/**
 * Anything that can hold photo binaries. The database only ever stores the key.
 * Swapping local disk for S3 is a configuration change, not a code change.
 */
public interface StorageService {

    /** Writes the object and returns the key it was stored under. */
    String store(String key, InputStream content, long contentLength, String contentType);

    /** A URL the browser can use directly, valid for a short window only. */
    String presignedUrl(String key);

    void delete(String key);

    /** Reads an object back. Only used by the local implementation's file endpoint. */
    StoredContent load(String key);

    String type();
}
