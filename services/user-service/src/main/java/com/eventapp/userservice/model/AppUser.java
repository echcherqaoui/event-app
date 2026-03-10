package com.eventapp.userservice.model;

import com.eventapp.userservice.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Setter @Getter
@Accessors(chain = true)
@EntityListeners(AuditingEntityListener.class)
public class AppUser {

    @Id
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean profileComplete = false;
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Column(length = 1000)
    private String bio;
    private LocalDate birthDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}