package com.paywave.auth.client;

import java.util.UUID;

public record CreateProfileRequest(UUID userId, String fullName, String email, String phone) {
}
