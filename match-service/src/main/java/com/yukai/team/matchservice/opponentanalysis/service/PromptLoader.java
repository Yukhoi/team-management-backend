package com.yukai.team.matchservice.opponentanalysis.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {

    private static final String SYSTEM_PROMPT_PATH = "prompts/opponent-analysis-system-prompt.txt";

    private final String opponentAnalysisSystemPrompt;

    public PromptLoader() {
        try {
            byte[] bytes = new ClassPathResource(SYSTEM_PROMPT_PATH).getInputStream().readAllBytes();
            String prompt = new String(bytes, StandardCharsets.UTF_8);
            if (prompt.isBlank()) {
                throw new IllegalStateException("Opponent analysis system prompt is empty");
            }
            this.opponentAnalysisSystemPrompt = prompt;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load opponent analysis system prompt", exception);
        }
    }

    public String opponentAnalysisSystemPrompt() {
        return opponentAnalysisSystemPrompt;
    }
}
