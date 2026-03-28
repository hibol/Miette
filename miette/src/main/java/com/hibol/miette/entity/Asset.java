package com.hibol.miette.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "asset")
public class Asset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column
    private String path;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Transient
    public String getThumbPath() {
        if (path == null) return null;
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(0, dot) + "_thumb" + path.substring(dot) : path + "_thumb";
    }

    @Transient
    public AssetType getType() {
        if (path == null) return AssetType.NOTE;
        String lower = path.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp")
                || lower.endsWith(".gif")) return AssetType.PHOTO;
        if (lower.endsWith(".pdf")) return AssetType.PDF;
        if (lower.endsWith(".mp4") || lower.endsWith(".mov")
                || lower.endsWith(".avi")) return AssetType.VIDEO;
        return AssetType.FILE;
    }

    public enum AssetType { NOTE, PHOTO, VIDEO, PDF, FILE }
}
