package com.hibol.miette.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

@Data
@Entity
@Table(name = "asset")
public class Asset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column
    private String path;

    @Column @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    @Column
    private String description;

    @Transient
    public AssetType getType() {
        if (path == null) return AssetType.NOTE;
        
        try {
            String mimeType = Files.probeContentType(Paths.get(path));
            if (mimeType == null) return AssetType.FILE;
            
            if (mimeType.equals("application/pdf")) {
                return AssetType.PDF;
            }
            if (mimeType.startsWith("image/")) {
                return AssetType.PHOTO;
            }
            if (mimeType.startsWith("video/")) {
                return AssetType.VIDEO;
            }
            return AssetType.FILE;
        } catch (IOException e) {
            return AssetType.FILE;
        }
    }

    
    public enum AssetType { NOTE, PHOTO, VIDEO, PDF, FILE }
}