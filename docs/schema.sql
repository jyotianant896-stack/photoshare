-- Reference schema for the photo sharing platform (MySQL 8).
--
-- The running application creates these tables itself through Hibernate
-- (spring.jpa.hibernate.ddl-auto=update). This file is the same schema written
-- out explicitly: useful for review, and the starting point for Flyway
-- migrations when this moves past a single developer.

CREATE DATABASE IF NOT EXISTS photoshare
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE photoshare;

-- ---------------------------------------------------------------- users
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,   -- BCrypt, never the plain password
    role          VARCHAR(20)  NOT NULL,   -- ADMIN | TEAM_MEMBER
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------- events
CREATE TABLE events (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    event_date  DATE         NULL,
    owner_id    BIGINT       NOT NULL,     -- the admin who created it
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_events_owner (owner_id),
    CONSTRAINT fk_events_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- ------------------------------------------------------- event membership
-- Who is allowed to work on which event. A user with no row here and no
-- ownership cannot see the event at all.
CREATE TABLE event_members (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    event_id BIGINT      NOT NULL,
    user_id  BIGINT      NOT NULL,
    added_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_member (event_id, user_id),
    CONSTRAINT fk_event_members_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_event_members_user  FOREIGN KEY (user_id)  REFERENCES users (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------- photos
-- Metadata only. storage_key points into object storage (local disk or S3);
-- image bytes are never stored in MySQL.
CREATE TABLE photos (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    event_id          BIGINT       NOT NULL,
    uploaded_by       BIGINT       NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key       VARCHAR(400) NOT NULL,   -- events/{eventId}/{uuid}.jpg
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_photos_event (event_id),
    KEY idx_photos_uploader (uploaded_by),
    CONSTRAINT fk_photos_event    FOREIGN KEY (event_id)    REFERENCES events (id),
    CONSTRAINT fk_photos_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id)
) ENGINE = InnoDB;

-- -------------------------------------------------------------- galleries
-- One per event. Exists as an unpublished draft as soon as the admin opens the
-- curation screen; becomes reachable only when published is true.
CREATE TABLE galleries (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    event_id     BIGINT       NOT NULL,
    slug         VARCHAR(32)  NOT NULL,    -- the /g/{slug} part of the share link
    title        VARCHAR(160) NOT NULL,
    pin_hash     VARCHAR(100) NULL,        -- BCrypt; the plain PIN is shown once
    published    BIT(1)       NOT NULL DEFAULT b'0',
    published_at DATETIME(6)  NULL,
    expires_at   DATETIME(6)  NULL,
    view_count   BIGINT       NOT NULL DEFAULT 0,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_galleries_slug (slug),
    UNIQUE KEY uk_galleries_event (event_id),
    CONSTRAINT fk_galleries_event FOREIGN KEY (event_id) REFERENCES events (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------- the selection
-- Which photos the client actually sees. Selecting is a row here, not a copy
-- of the file, so curation is cheap and reversible.
CREATE TABLE gallery_photos (
    gallery_id BIGINT NOT NULL,
    photo_id   BIGINT NOT NULL,
    PRIMARY KEY (gallery_id, photo_id),
    KEY idx_gallery_photos_photo (photo_id),
    CONSTRAINT fk_gallery_photos_gallery FOREIGN KEY (gallery_id) REFERENCES galleries (id),
    CONSTRAINT fk_gallery_photos_photo   FOREIGN KEY (photo_id)   REFERENCES photos (id)
) ENGINE = InnoDB;
