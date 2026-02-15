package com.eventapp.userservice.service;

import com.eventapp.userservice.dto.request.ProfileUpdateRequest;
import com.eventapp.userservice.dto.response.UserResponse;

public interface IUserService {
    void syncUser();

    UserResponse completeProfile(ProfileUpdateRequest request);

    UserResponse updateProfile(ProfileUpdateRequest request);
}
