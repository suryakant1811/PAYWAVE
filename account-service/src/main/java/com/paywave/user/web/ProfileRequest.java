package com.paywave.user.web;

import java.util.UUID;

public record ProfileRequest(UUID userId, String fullName, String email, String phone) {
}
