package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.application.AvatarUploadUrl;

public record AvatarUploadUrlResponse(String uploadUrl, String avatarS3Key, long expiresInSeconds) {

    public static AvatarUploadUrlResponse from(AvatarUploadUrl uploadUrl) {
        return new AvatarUploadUrlResponse(
                uploadUrl.uploadUrl(), uploadUrl.avatarS3Key(), uploadUrl.expiresInSeconds());
    }
}
