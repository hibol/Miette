package com.hibol.miette.service;

import com.hibol.miette.entity.AppSetting;
import com.hibol.miette.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppSettingService {

    private final AppSettingRepository repo;

    public List<AppSetting> findAll() {
        return repo.findAll();
    }

    public String getValue(String key, String defaultValue) {
        return repo.findById(key).map(AppSetting::getValue).orElse(defaultValue);
    }

    @Transactional
    public void seedIfAbsent(String key, String value, String description) {
        if (!repo.existsById(key)) {
            AppSetting setting = new AppSetting();
            setting.setKey(key);
            setting.setValue(value);
            setting.setDescription(description);
            repo.save(setting);
        }
    }

    @Transactional
    public void update(String key, String value) {
        AppSetting setting = repo.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Clé inconnue : " + key));
        setting.setValue(value);
        repo.save(setting);
    }
}
