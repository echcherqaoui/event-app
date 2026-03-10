package com.eventapp.apigateway.controller;

import com.eventapp.apigateway.dto.UserPageResponse;
import com.eventapp.apigateway.service.IIdentityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final IIdentityService keycloakService;

    public AdminController(IIdentityService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @PostMapping("/promote/{userId}")
    public Mono<ResponseEntity<String>> promote(@PathVariable("userId") String userId,
                                                @RequestParam("role") String role) {
        return keycloakService.assignRole(userId, role)
              .thenReturn(ResponseEntity.ok("User promoted successfully"))
              .onErrorReturn(ResponseEntity.status(404).body("User or Role not found"));
    }

    @GetMapping("/users")
    public Mono<UserPageResponse> getUsers(@RequestParam(name = "page", defaultValue = "0") int page,
                                           @RequestParam(name = "size", defaultValue = "20") int size) {
        return keycloakService.getPagedUsers(page, size);
    }
}