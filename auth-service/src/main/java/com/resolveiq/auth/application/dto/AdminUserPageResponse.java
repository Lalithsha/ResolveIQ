package com.resolveiq.auth.application.dto;

import java.util.List;

public record AdminUserPageResponse(
    List<UserProfileDto> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
