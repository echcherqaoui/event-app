package com.eventapp.userservice.service.impl;

import com.eventapp.userservice.dto.request.ProfileUpdateRequest;
import com.eventapp.userservice.dto.response.UserResponse;
import com.eventapp.userservice.exceptions.domain.ProfileAlreadyCompleteException;
import com.eventapp.userservice.exceptions.domain.UserNotFoundException;
import com.eventapp.userservice.mapper.UserMapper;
import com.eventapp.userservice.model.AppUser;
import com.eventapp.userservice.repository.UserRepository;
import com.eventapp.userservice.service.IKeycloakIntegrationService;
import com.eventapp.userservice.service.IUserService;
import com.eventapp.sharedutils.security.ISecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final IKeycloakIntegrationService keycloakIntegrationService;
    private final ISecurityService securityService;

    private AppUser updateExistingUser(AppUser user, Map<String, Object> claims) {
        user.setFirstName(claims.getOrDefault("given_name", user.getFirstName()).toString());
        user.setLastName(claims.getOrDefault("family_name", user.getLastName()).toString());
        user.setEmail(claims.getOrDefault("email", user.getEmail()).toString());

        return user;
    }

    private AppUser getUserById(String id){
        return userRepository.findById(id)
              .orElseThrow(() -> new UserNotFoundException(id));
    }

    private boolean updateLocalData(AppUser user, ProfileUpdateRequest req) {
        boolean nameChanged = false;

        // Names (Keycloak + DB)
        if (req.firstName() != null && !req.firstName().equals(user.getFirstName())) {
            user.setFirstName(req.firstName());
            nameChanged = true;
        }

        if (req.lastName() != null && !req.lastName().equals(user.getLastName())) {
            user.setLastName(req.lastName());
            nameChanged = true;
        }

        // Other fields (DB Only)
        Optional.ofNullable(req.bio()).ifPresent(user::setBio);
        Optional.ofNullable(req.gender()).ifPresent(user::setGender);
        Optional.ofNullable(req.phoneNumber()).ifPresent(user::setPhoneNumber);
        Optional.ofNullable(req.birthDate()).ifPresent(user::setBirthDate);

        return nameChanged;
    }

    @Transactional
    @Override
    public void syncUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Map<String, Object> claims = jwtAuth.getToken().getClaims();
            String idpId = jwt.getSubject();

            AppUser user = userRepository.findById(idpId)
                  .map(existing -> updateExistingUser(existing, claims))
                  .orElseGet(() -> userMapper.fromTokenAttributes(claims));

            userRepository.save(user);
        }
    }

    @Transactional
    @Override
    public UserResponse completeProfile(ProfileUpdateRequest request) {
        String currentUserId = securityService.getAuthenticatedUserId();

        AppUser user = getUserById(currentUserId);

        if (user.isProfileComplete())
            throw new ProfileAlreadyCompleteException();

        // Mandatory fields for EventApp onboarding
        user.setPhoneNumber(request.phoneNumber())
              .setBirthDate(request.birthDate())
              .setGender(request.gender());

        if (request.bio() != null) user.setBio(request.bio());

        user.setProfileComplete(true);

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    @Override
    public UserResponse updateProfile(ProfileUpdateRequest request) {
        String currentUserId = securityService.getAuthenticatedUserId();
        AppUser user = getUserById(currentUserId);

        // Update Profile & Detect if Keycloak sync is needed
        boolean nameChanged = updateLocalData(user, request);

        AppUser savedUser = userRepository.save(user);

        // Trigger Async Sync
        if (nameChanged) keycloakIntegrationService.syncProfile(currentUserId, request);

        return UserResponse.from(savedUser);
    }
}