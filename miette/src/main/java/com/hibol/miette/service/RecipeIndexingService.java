package com.hibol.miette.service;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import com.hibol.miette.entity.Recipe;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeIndexingService {

    private final EntityManagerFactory entityManagerFactory;

    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup() {
        rebuildIndex();
    }

    public void rebuildIndex() {
        log.info("🔍 Rebuilding search index...");
        try {
            Search.mapping(entityManagerFactory)
                  .scope(Recipe.class)
                  .massIndexer()
                  .startAndWait();
            log.info("✅ Search index rebuilt");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("⚠️ Search index rebuild interrupted", e);
        }
    }
}
