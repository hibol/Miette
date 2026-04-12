package com.hibol.miette.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "app_setting")
public class AppSetting {

    @Id
    @Column(name = "setting_key", nullable = false, unique = true)
    private String key;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String value;

    @Column
    private String description;
}
