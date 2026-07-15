package com.mooc.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mooc.app.converter.StringListConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    public enum State {
        active, locked, deleted, email_unverified
    }

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(unique = true, length = 20)
    private String username;

    @Column(length = 30)
    private String nickname;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private State state = State.active;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(length = 500)
    private String bio;

    @Convert(converter = StringListConverter.class)
    @Column(name = "interest_tags", columnDefinition = "TEXT")
    private List<String> interestTags = new ArrayList<>();

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getInterestTags() { return interestTags; }
    public void setInterestTags(List<String> interestTags) { this.interestTags = interestTags != null ? interestTags : new ArrayList<>(); }
}
