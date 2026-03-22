package com.hibol.miette.controller.api;

import com.hibol.miette.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.hibol.miette.entity.Tag;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagApiController {

    private final TagRepository tagRepo;

    @GetMapping
    public List<String> tags() {
        return tagRepo.findAll().stream()
            .map(Tag::getLabel)
            .sorted()
            .toList();
    }
}
