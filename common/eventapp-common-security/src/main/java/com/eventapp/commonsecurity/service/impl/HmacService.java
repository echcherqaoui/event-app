package com.eventapp.commonsecurity.service.impl;

import com.eventapp.commonsecurity.service.IEventAuthenticator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class HmacService implements IEventAuthenticator {

    private final String secret;

    public HmacService(String secret) {
        this.secret = secret;
    }

    private static final String ALGORITHM = "HmacSHA256";

    @Override
    public String sign(String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                  secret.getBytes(StandardCharsets.UTF_8),
                  ALGORITHM
            );

            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKeySpec);

            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64
                  .getEncoder()
                  .encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC signature", e);
        }
    }

    @Override
    public boolean verify(String data,
                          String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) return false;

        String calculated = sign(data);

        // MessageDigest.isEqual used for constant-time comparison (prevents timing attacks)
        return MessageDigest.isEqual(
              calculated.getBytes(StandardCharsets.UTF_8),
              providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}