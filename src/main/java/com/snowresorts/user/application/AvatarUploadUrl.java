package com.snowresorts.user.application;

/** Result of requesting a presigned avatar upload: the PUT URL, the target key and its TTL. */
public record AvatarUploadUrl(String uploadUrl, String avatarS3Key, long expiresInSeconds) {
}
