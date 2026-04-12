package com.hibol.miette.controller.api;

import com.hibol.miette.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class AppSettingApiController {

    private final AppSettingService appSettingService;

    @GetMapping("/{key}")
    public ResponseEntity<String> get(@PathVariable String key) {
        String value = appSettingService.getValue(key, null);
        if (value == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(value);
    }
}
