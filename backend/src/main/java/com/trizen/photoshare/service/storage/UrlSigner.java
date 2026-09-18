package com.trizen.photoshare.service.storage;

import com.trizen.photoshare.config.AppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Signs local file URLs the same way S3 presigns its own: the link carries an
 * expiry plus an HMAC over (key, expiry), so a leaked link stops working and a
 * guessed key is useless without the server secret.
 */
@Component
public class UrlSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public UrlSigner(AppProperties properties) {
        this.secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String key, long expiresAtEpochSeconds) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            byte[] digest = mac.doFinal((key + ":" + expiresAtEpochSeconds).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign file URL", ex);
        }
    }

    public boolean isValid(String key, long expiresAtEpochSeconds, String signature) {
        if (signature == null || Instant.now().getEpochSecond() > expiresAtEpochSeconds) {
            return false;
        }
        String expected = sign(key, expiresAtEpochSeconds);
        // Constant time compare so the signature cannot be probed byte by byte.
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
