package com.hibol.miette.dto.api.response;

import java.time.LocalDateTime;

public record AssetDto(Long id, LocalDateTime date, String description, String type, String url, String thumbUrl) {}
