package com.fitness.tracker.dto;

/** Sign-in options the website should offer. googleClientId is null when Google sign-in isn't configured. */
public record AuthProvidersResponse(String googleClientId) {
}
