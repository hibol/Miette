package com.hibol.miette.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hibol.miette.config.UmamiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UmamiService {

    private final UmamiProperties props;
    private final ObjectMapper objectMapper;

    public record UmamiStats(long pageviews, long visitors) {}
    public record TopPage(String path, long views) {}
    public record UmamiData(UmamiStats stats, List<TopPage> topPages) {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .version(java.net.http.HttpClient.Version.HTTP_1_1)
        .build();

    public UmamiData fetchStats() {
        if (props.getUrl() == null || props.getUrl().isBlank()) {
            log.debug("Umami non configuré (URL manquante)");
            return null;
        }
        try {
            String token = authenticate();
            if (token == null) {
                log.warn("Umami : authentification échouée — token absent de la réponse");
                return null;
            }

            long endAt = Instant.now().toEpochMilli();
            long startAt = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();

            UmamiStats stats = fetchSummary(token, startAt, endAt);
            List<TopPage> topPages = fetchTopPages(token, startAt, endAt);

            log.debug("Umami stats récupérées : {} pages vues, {} visiteurs, {} top pages",
                stats.pageviews(), stats.visitors(), topPages.size());
            return new UmamiData(stats, topPages);
        } catch (Exception e) {
            log.warn("Umami : impossible de récupérer les stats — {}", e.getMessage());
            return null;
        }
    }

    private String authenticate() throws Exception {
        String body = objectMapper.writeValueAsString(
            Map.of("username", props.getUsername(), "password", props.getPassword())
        );
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(props.getUrl() + "/api/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Umami : auth HTTP {} — {}", response.statusCode(), response.body());
            return null;
        }
        JsonNode json = objectMapper.readTree(response.body());
        String token = json.path("token").asText(null);
        if (token == null) {
            log.warn("Umami : champ 'token' absent de la réponse d'auth — réponse : {}", response.body());
        }
        return token;
    }

    private UmamiStats fetchSummary(String token, long startAt, long endAt) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(props.getUrl() + "/api/websites/" + props.getWebsiteId()
                + "/stats?startAt=" + startAt + "&endAt=" + endAt))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Umami : stats HTTP {} — {}", response.statusCode(), response.body());
            return new UmamiStats(0, 0);
        }
        JsonNode json = objectMapper.readTree(response.body());
        long pageviews = json.path("pageviews").path("value").asLong(0);
        long visitors = json.path("visitors").path("value").asLong(0);
        return new UmamiStats(pageviews, visitors);
    }

    private List<TopPage> fetchTopPages(String token, long startAt, long endAt) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(props.getUrl() + "/api/websites/" + props.getWebsiteId()
                + "/metrics?startAt=" + startAt + "&endAt=" + endAt + "&type=path&limit=10"))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Umami : top pages HTTP {} — {}", response.statusCode(), response.body());
            return List.of();
        }
        JsonNode json = objectMapper.readTree(response.body());
        List<TopPage> pages = new ArrayList<>();
        if (json.isArray()) {
            json.forEach(node -> pages.add(new TopPage(
                node.path("x").asText(),
                node.path("y").asLong(0)
            )));
        }
        return pages;
    }
}
