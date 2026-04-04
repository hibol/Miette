package com.hibol.miette.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "umami")
public class UmamiProperties {
    private String url;
    private String username;
    private String password;
    private String websiteId;
}
