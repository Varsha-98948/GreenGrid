package com.greengrid.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.greengrid.entity.Problem;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces the structured metadata.json that sits next to each solution —
 * this is what lets GreenGrid (or any other tool) re-parse a repository
 * later without depending on scraping the README's prose.
 */
@Service
public class MetadataGeneratorService {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String generate(Problem problem) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", problem.getTitle());
        data.put("platform", problem.getPlatform());
        data.put("problemUrl", problem.getProblemUrl());
        data.put("difficulty", problem.getDifficulty().name());
        data.put("language", problem.getLanguage());
        data.put("topics", problem.getTags().stream().map(t -> t.getName()).toList());
        data.put("timeComplexity", problem.getTimeComplexity());
        data.put("spaceComplexity", problem.getSpaceComplexity());
        data.put("solvedDate", problem.getSolvedDate().toString());
        data.put("source", "GreenGrid");

        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize problem metadata", e);
        }
    }
}
