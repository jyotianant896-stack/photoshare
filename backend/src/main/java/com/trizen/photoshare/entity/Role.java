package com.trizen.photoshare.entity;

/**
 * Application level roles. ADMIN owns events and publishes galleries,
 * TEAM_MEMBER can only upload to events they are assigned to.
 */
public enum Role {
    ADMIN,
    TEAM_MEMBER
}
