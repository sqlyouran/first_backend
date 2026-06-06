package com.mooc.app.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public void add(String jti) {
        blacklist.add(jti);
    }

    public boolean isBlacklisted(String jti) {
        return blacklist.contains(jti);
    }
}
