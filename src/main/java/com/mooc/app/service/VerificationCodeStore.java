package com.mooc.app.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerificationCodeStore {

    private record CodeEntry(String code, Instant expiresAt) {}

    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();

    public void save(String email, String code, long ttlSeconds) {
        store.put(email.toLowerCase(), new CodeEntry(code, Instant.now().plusSeconds(ttlSeconds)));
    }

    public Optional<String> getCode(String email) {
        CodeEntry entry = store.get(email.toLowerCase());
        if (entry == null) return Optional.empty();
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return Optional.empty();
        }
        return Optional.of(entry.code());
    }

    public boolean isExpired(String email) {
        CodeEntry entry = store.get(email.toLowerCase());
        if (entry == null) return true;
        return Instant.now().isAfter(entry.expiresAt());
    }

    public void remove(String email) {
        store.remove(email.toLowerCase());
    }
}
