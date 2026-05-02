package com.hibol.miette.controller;

import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.RecipeRepository.RecipeSitemap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SitemapController {

    private final RecipeRepository recipeRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<String[]> STATIC_PAGES = List.of(
        new String[]{"/",         "monthly", "1.0"},
        new String[]{"/recettes", "weekly",  "0.9"},
        new String[]{"/a-propos", "monthly", "0.5"}
    );

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);

        for (String[] page : STATIC_PAGES) {
            xml.append("  <url>\n")
               .append("    <loc>").append(base).append(page[0]).append("</loc>\n")
               .append("    <changefreq>").append(page[1]).append("</changefreq>\n")
               .append("    <priority>").append(page[2]).append("</priority>\n")
               .append("  </url>\n");
        }

        for (RecipeSitemap r : recipeRepository.findAllForSitemap()) {
            String lastmod = r.getUpdatedAt() != null ? r.getUpdatedAt().format(DATE_FMT) : "";
            xml.append("  <url>\n")
               .append("    <loc>").append(base).append("/recette/").append(r.getId()).append("</loc>\n");
            if (!lastmod.isEmpty()) {
                xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
            }
            xml.append("    <changefreq>monthly</changefreq>\n")
               .append("    <priority>0.8</priority>\n")
               .append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }
}
