package com.yukai.team.matchservice.opponentanalysis.dto.openrouter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterChatCompletionRequest(
        String model,
        List<Message> messages,
        BigDecimal temperature,
        @JsonProperty("max_tokens")
        Integer maxTokens,
        Reasoning reasoning
) {
    public record Message(String role, String content) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Reasoning(
            Boolean enabled,
            String effort,
            Boolean exclude,
            @JsonProperty("max_tokens")
            Integer maxTokens
    ) {
        public static Reasoning disabled() {
            return new Reasoning(false, "none", true, null);
        }
    }
}
