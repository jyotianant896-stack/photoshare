package com.trizen.photoshare.service.storage;

import org.springframework.core.io.Resource;

public record StoredContent(Resource resource, String contentType, long contentLength) {
}
