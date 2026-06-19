package com.mooc.app.service;

import com.mooc.app.dto.UpdateProfileRequest;
import com.mooc.app.dto.response.InterestTagListResponse;
import com.mooc.app.dto.response.ProfileResponse;
import com.mooc.app.dto.response.PublicProfileResponse;
import com.mooc.app.entity.InterestTag;
import com.mooc.app.entity.UserEntity;
import com.mooc.app.exception.ProfileException;
import com.mooc.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");
    private static final Pattern NICKNAME_INVALID_CHARS = Pattern.compile("[<>\"]");

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ProfileResponse getMyProfile(UUID userId, String requestId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ProfileException(HttpStatus.NOT_FOUND, "not_found", "User not found"));

        if (user.isDeleted() || user.getState() == UserEntity.State.deleted) {
            throw new ProfileException(HttpStatus.NOT_FOUND, "not_found", "User not found");
        }

        return toProfileResponse(user, requestId);
    }

    public ProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request, String requestId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ProfileException(HttpStatus.NOT_FOUND, "not_found", "User not found"));

        if (user.isDeleted() || user.getState() == UserEntity.State.deleted) {
            throw new ProfileException(HttpStatus.NOT_FOUND, "not_found", "User not found");
        }

        // Username: set-once, immutable after set. If already set and request includes it, silently ignore.
        if (request.username() != null && !request.username().isBlank()) {
            if (user.getUsername() == null) {
                // Validate format
                if (!USERNAME_PATTERN.matcher(request.username()).matches()) {
                    throw new ProfileException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                            "Username must be 3-20 lowercase letters, digits, or underscores");
                }
                // Check uniqueness
                if (userRepository.existsByUsername(request.username())) {
                    throw new ProfileException(HttpStatus.CONFLICT, "username_taken",
                            "Username is already taken");
                }
                user.setUsername(request.username());
            }
            // If user.getUsername() != null, silently ignore
        }

        // Nickname: null = no change, "" = clear to null, non-empty = validate + set
        if (request.nickname() != null) {
            String trimmed = request.nickname().trim();
            if (trimmed.isEmpty()) {
                user.setNickname(null);
            } else {
                if (trimmed.length() < 2) {
                    throw new ProfileException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                            "Nickname must be at least 2 characters");
                }
                if (NICKNAME_INVALID_CHARS.matcher(trimmed).find()) {
                    throw new ProfileException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                            "Nickname must not contain <, > or \" characters");
                }
                user.setNickname(trimmed);
            }
        }

        // Optional fields: null = no change, "" / empty = clear to null, non-empty = set
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl());
        }

        if (request.bio() != null) {
            user.setBio(request.bio().isEmpty() ? null : request.bio());
        }

        if (request.interestTags() != null) {
            if (request.interestTags().isEmpty()) {
                user.setInterestTags(null);
            } else {
                // Validate each tag
                if (request.interestTags().size() > 10) {
                    throw new ProfileException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                            "Maximum 10 interest tags allowed");
                }
                for (String tag : request.interestTags()) {
                    if (!InterestTag.isValid(tag)) {
                        throw new ProfileException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                                "Invalid interest tag: " + tag);
                    }
                }
                user.setInterestTags(new ArrayList<>(request.interestTags()));
            }
        }

        try {
            UserEntity saved = userRepository.save(user);
            log.info("Profile updated for user {}", userId);
            return toProfileResponse(saved, requestId);
        } catch (DataIntegrityViolationException e) {
            throw new ProfileException(HttpStatus.CONFLICT, "username_taken",
                    "Username is already taken");
        }
    }

    public PublicProfileResponse getPublicProfile(String username, String requestId) {
        UserEntity user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ProfileException(HttpStatus.NOT_FOUND, "not_found",
                        "User not found"));

        if (user.getState() == UserEntity.State.deleted) {
            throw new ProfileException(HttpStatus.NOT_FOUND, "not_found", "User not found");
        }

        return toPublicProfileResponse(user, requestId);
    }

    public InterestTagListResponse getInterestTags(String requestId) {
        List<InterestTagListResponse.TagItem> tags = Arrays.stream(InterestTag.values())
                .map(t -> new InterestTagListResponse.TagItem(t.name(), t.getLabel(), t.getCategory()))
                .toList();
        return new InterestTagListResponse(requestId, tags);
    }

    private ProfileResponse toProfileResponse(UserEntity user, String requestId) {
        return new ProfileResponse(
                requestId,
                user.getId().toString(),
                user.getEmail(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getInterestTags(),
                user.getCreatedAt().toString()
        );
    }

    private PublicProfileResponse toPublicProfileResponse(UserEntity user, String requestId) {
        return new PublicProfileResponse(
                requestId,
                user.getId().toString(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getInterestTags(),
                user.getCreatedAt().toString()
        );
    }
}
