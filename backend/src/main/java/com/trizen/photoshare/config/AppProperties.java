package com.trizen.photoshare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Every environment specific knob lives here so nothing is hard coded in the code
 * and nothing secret has to be committed.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String publicBaseUrl = "http://localhost:8080";
    private String frontendBaseUrl = "http://localhost:5173";
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

    private final Jwt jwt = new Jwt();
    private final Storage storage = new Storage();
    private final Upload upload = new Upload();
    private final Gallery gallery = new Gallery();
    private final Demo demo = new Demo();

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }

    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public void setFrontendBaseUrl(String frontendBaseUrl) { this.frontendBaseUrl = frontendBaseUrl; }

    public List<String> getCorsAllowedOrigins() { return corsAllowedOrigins; }
    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) { this.corsAllowedOrigins = corsAllowedOrigins; }

    public Jwt getJwt() { return jwt; }
    public Storage getStorage() { return storage; }
    public Upload getUpload() { return upload; }
    public Gallery getGallery() { return gallery; }
    public Demo getDemo() { return demo; }

    public static class Jwt {
        /** Must be at least 32 characters. Injected from the environment in every real deployment. */
        private String secret = "change-me-in-production-please-use-32-bytes-min";
        private long accessTokenMinutes = 720;
        private long galleryTokenMinutes = 120;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public long getAccessTokenMinutes() { return accessTokenMinutes; }
        public void setAccessTokenMinutes(long accessTokenMinutes) { this.accessTokenMinutes = accessTokenMinutes; }

        public long getGalleryTokenMinutes() { return galleryTokenMinutes; }
        public void setGalleryTokenMinutes(long galleryTokenMinutes) { this.galleryTokenMinutes = galleryTokenMinutes; }
    }

    public static class Storage {
        /** local or s3 */
        private String type = "local";
        private String localDir = "./data/uploads";
        private String bucket = "";
        private String region = "ap-south-1";
        /** Optional override for S3 compatible services such as MinIO. */
        private String endpoint = "";
        private long signedUrlMinutes = 30;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getLocalDir() { return localDir; }
        public void setLocalDir(String localDir) { this.localDir = localDir; }

        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public long getSignedUrlMinutes() { return signedUrlMinutes; }
        public void setSignedUrlMinutes(long signedUrlMinutes) { this.signedUrlMinutes = signedUrlMinutes; }
    }

    public static class Upload {
        private List<String> allowedContentTypes =
                new ArrayList<>(List.of("image/jpeg", "image/png", "image/webp", "image/heic"));
        private long maxFileSizeBytes = 25L * 1024 * 1024;
        private int maxFilesPerRequest = 50;

        public List<String> getAllowedContentTypes() { return allowedContentTypes; }
        public void setAllowedContentTypes(List<String> allowedContentTypes) { this.allowedContentTypes = allowedContentTypes; }

        public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
        public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }

        public int getMaxFilesPerRequest() { return maxFilesPerRequest; }
        public void setMaxFilesPerRequest(int maxFilesPerRequest) { this.maxFilesPerRequest = maxFilesPerRequest; }
    }

    public static class Gallery {
        private int pinLength = 6;
        private int maxPinAttempts = 5;
        private long pinLockoutMinutes = 15;

        public int getPinLength() { return pinLength; }
        public void setPinLength(int pinLength) { this.pinLength = pinLength; }

        public int getMaxPinAttempts() { return maxPinAttempts; }
        public void setMaxPinAttempts(int maxPinAttempts) { this.maxPinAttempts = maxPinAttempts; }

        public long getPinLockoutMinutes() { return pinLockoutMinutes; }
        public void setPinLockoutMinutes(long pinLockoutMinutes) { this.pinLockoutMinutes = pinLockoutMinutes; }
    }

    public static class Demo {
        /** Seeds a demo admin, team member and event on an empty database. */
        private boolean seed = true;
        private String adminEmail = "admin@trizen-ai.com";
        private String adminPassword = "Admin@12345";
        private String memberEmail = "photographer@trizen-ai.com";
        private String memberPassword = "Member@12345";

        public boolean isSeed() { return seed; }
        public void setSeed(boolean seed) { this.seed = seed; }

        public String getAdminEmail() { return adminEmail; }
        public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

        public String getMemberEmail() { return memberEmail; }
        public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }

        public String getMemberPassword() { return memberPassword; }
        public void setMemberPassword(String memberPassword) { this.memberPassword = memberPassword; }
    }
}
