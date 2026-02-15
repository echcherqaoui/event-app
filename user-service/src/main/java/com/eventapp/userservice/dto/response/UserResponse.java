package com.eventapp.userservice.dto.response;

import com.eventapp.userservice.enums.Gender;
import com.eventapp.userservice.model.AppUser;

import java.time.LocalDate;

public record UserResponse(String email,
                           String firstName,
                           String lastName,
                           String bio,
                           Gender gender,
                           String phoneNumber,
                           LocalDate birthDate,
                           boolean profileComplete) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(
              user.getEmail(),
              user.getFirstName(),
              user.getLastName(),
              user.getBio(),
              user.getGender(),
              user.getPhoneNumber(),
              user.getBirthDate(),
              user.isProfileComplete()
        );
    }
}