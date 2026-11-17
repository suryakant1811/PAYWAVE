package com.paywave.auth.client;

import com.paywave.user.service.UserProfileService;
import com.paywave.user.web.ProfileRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserServiceClient {

    private final UserProfileService profiles;

    public UserServiceClient(UserProfileService profiles) {
        this.profiles = profiles;
    }

    public void createProfile(CreateProfileRequest request) {
        profiles.create(new ProfileRequest(request.userId(), request.fullName(), request.email(), request.phone()));
    }

    public void deleteProfile(UUID userId) {
        profiles.delete(userId);
    }
}
