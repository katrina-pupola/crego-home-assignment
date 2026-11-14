package com.crego.backend.notification;

/** Published when BO staff approve or reject a change request. */
public record ChangeRequestDecided(String email, String fullName, boolean approved, String comment) {
}
