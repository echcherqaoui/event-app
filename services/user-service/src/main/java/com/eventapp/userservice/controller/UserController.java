package com.eventapp.userservice.controller;

import com.eventapp.userservice.dto.request.ProfileUpdateRequest;
import com.eventapp.userservice.dto.response.UserResponse;
import com.eventapp.userservice.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.base-path}/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @PostMapping("/sync")
    public ResponseEntity<Void> sync() {
        userService.syncUser();

        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/complete-profile")
    public ResponseEntity<UserResponse> completeProfile(@RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.completeProfile(request));
    }

    @PutMapping("/update-profile")
    public ResponseEntity<UserResponse> updateProfile(@RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

}