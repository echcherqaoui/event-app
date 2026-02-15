package com.eventapp.userservice.dto.request;

import com.eventapp.userservice.enums.Gender;

import java.time.LocalDate;

public record ProfileUpdateRequest(String firstName,
                                   String lastName,
                                   Gender gender,
                                   String bio,
                                   String phoneNumber,
                                   LocalDate birthDate) {
}
