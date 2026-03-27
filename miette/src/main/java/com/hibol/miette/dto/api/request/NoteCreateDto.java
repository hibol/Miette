package com.hibol.miette.dto.api.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record NoteCreateDto(@NotNull LocalDateTime date, String description) {}
