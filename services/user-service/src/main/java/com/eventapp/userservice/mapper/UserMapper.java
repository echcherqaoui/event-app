package com.eventapp.userservice.mapper;

import com.eventapp.userservice.model.AppUser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserMapper {

    private String getClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return (value != null) ? value.toString() : "";
    }

    public AppUser fromTokenAttributes(Map<String, Object> claims) {
        return new AppUser()
              .setId(getClaim(claims, "sub"))
              .setEmail(getClaim(claims, "email"))
              .setFirstName(getClaim(claims, "given_name"))
              .setLastName(getClaim(claims, "family_name"));
    }
}