package com.eventapp.userservice.service.impl;

import com.eventapp.sharedutils.security.ISecurityService;
import com.eventapp.userservice.dto.request.ProfileUpdateRequest;
import com.eventapp.userservice.dto.response.UserResponse;
import com.eventapp.userservice.exceptions.domain.ProfileAlreadyCompleteException;
import com.eventapp.userservice.exceptions.domain.UserNotFoundException;
import com.eventapp.userservice.model.AppUser;
import com.eventapp.userservice.repository.UserRepository;
import com.eventapp.userservice.service.IKeycloakIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.eventapp.userservice.enums.Gender.MALE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Unit Tests")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private IKeycloakIntegrationService keycloakService;
    @Mock private ISecurityService securityService;

    @InjectMocks private UserServiceImpl userService;

    private static final String USER_ID = "sub-12345";
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AppUser()
              .setId(USER_ID)
              .setEmail("test@eventapp.com")
              .setFirstName("Ahmed")
              .setLastName("EDER")
              .setProfileComplete(false);
    }

    @Nested
    @DisplayName("Profile Completion Tests")
    class CompleteProfileTests {

        @Test
        @DisplayName("Should complete profile when valid request is provided")
        void shouldCompleteProfileAndPersistData_WhenValidRequestReceived() {
            var request = new ProfileUpdateRequest(
                  null,
                  null,
                  MALE,
                  "Born to code",
                  "+2126000000",
                  LocalDate.of(1980, 1, 1)
            );

            when(securityService.getAuthenticatedUserId())
                  .thenReturn(USER_ID);

            when(userRepository.findById(USER_ID))
                  .thenReturn(Optional.of(testUser));

            when(userRepository.save(any(AppUser.class)))
                  .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userService.completeProfile(request);

            assertThat(response).isNotNull();
            assertThat(testUser.isProfileComplete()).isTrue();
            assertThat(testUser.getBio()).isEqualTo("Born to code");
            assertThat(testUser.getGender()).isEqualTo(MALE);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw exception when profile is already marked as complete")
        void shouldThrowException_WhenProfileAlreadyComplete() {
            testUser.setProfileComplete(true);

            when(securityService.getAuthenticatedUserId())
                  .thenReturn(USER_ID);

            when(userRepository.findById(USER_ID))
                  .thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userService.completeProfile(mock(ProfileUpdateRequest.class)))
                  .isInstanceOf(ProfileAlreadyCompleteException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Profile Update Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update user and trigger Keycloak sync when names change")
        void shouldUpdateUserAndSyncWithKeycloak_WhenNameIsChanged() {
            var request = new ProfileUpdateRequest(
                  "Ayoub",
                  "WALI",
                  null,
                  "New Bio",
                  null,
                  null
            );

            when(securityService.getAuthenticatedUserId())
                  .thenReturn(USER_ID);

            when(userRepository.findById(USER_ID))
                  .thenReturn(Optional.of(testUser));

            when(userRepository.save(any(AppUser.class)))
                  .thenAnswer(i -> i.getArgument(0));

            userService.updateProfile(request);

            assertThat(testUser.getFirstName()).isEqualTo("Ayoub");
            assertThat(testUser.getLastName()).isEqualTo("WALI");

            verify(keycloakService, times(1)).syncProfile(USER_ID, request);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should persist local modifications and bypass Keycloak synchronization when identity remains unchanged")
        void shouldUpdateOnlyLocalData_WhenOnlyProfileFieldsAreModified() {
            var request = new ProfileUpdateRequest(
                  "Ahmed",
                  "EDER",
                  MALE,
                  "Updated Bio",
                  null,
                  null
            );

            when(securityService.getAuthenticatedUserId())
                  .thenReturn(USER_ID);

            when(userRepository.findById(USER_ID))
                  .thenReturn(Optional.of(testUser));

            when(userRepository.save(any(AppUser.class)))
                  .thenAnswer(i -> i.getArgument(0));

            userService.updateProfile(request);

            assertThat(testUser.getBio()).isEqualTo("Updated Bio");
            assertThat(testUser.getGender()).isEqualTo(MALE);
            verify(keycloakService, never()).syncProfile(anyString(), any());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when ID does not exist in database")
        void shouldThrowUserNotFoundException_WhenPrincipalIdDoesNotExist() {
            when(securityService.getAuthenticatedUserId()).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile(mock(ProfileUpdateRequest.class)))
                  .isInstanceOf(UserNotFoundException.class);
        }
    }
}