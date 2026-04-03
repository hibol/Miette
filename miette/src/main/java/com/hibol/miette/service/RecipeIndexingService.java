package com.hibol.miette.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RecipeIndexingService {

    @PersistenceContext
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup() {
        rebuildIndex();
    }

    public void rebuildIndex() {
        log.info("🔍 Rebuilding search index...");
        try {
            Search.session(entityManager)
                  .massIndexer()
                  .startAndWait();
            log.info("✅ Search index rebuilt");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("⚠️ Search index rebuild interrupted", e);
        }
    }
}
