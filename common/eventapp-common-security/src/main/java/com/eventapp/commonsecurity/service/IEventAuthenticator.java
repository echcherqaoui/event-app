package com.eventapp.commonsecurity.service;

public interface IEventAuthenticator {
    String sign(String data);

    boolean verify(String data,
                   String signature);
}