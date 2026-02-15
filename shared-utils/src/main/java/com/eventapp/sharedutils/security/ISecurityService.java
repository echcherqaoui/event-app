package com.eventapp.sharedutils.security;

public interface ISecurityService {
    String getAuthenticatedUserId();

    String getUserEmail();
}
