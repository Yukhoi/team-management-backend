package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.matchservice.opponentanalysis.config.OpponentAiProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.AnalysisFinding;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.GeneratedOpponentReport;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentAnalysisInput;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentReportGenerationResult;
import com.yukai.team.matchservice.opponentanalysis.dto.openrouter.OpenRouterChatCompletionRequest;
import com.yukai.team.matchservice.opponentanalysis.dto.openrouter.OpenRouterChatCompletionResponse;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class OpenRouterOpponentReportGenerator implements OpponentReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterOpponentReportGenerator.class);
    private static final String SYSTEM_ROLE = "system";
    private static final String USER_ROLE = "user";
    private static final int MAX_RETRY_OUTPUT_TOKENS = 6000;
    private static final Pattern LONG_ENGLISH_SENTENCE = Pattern.compile("(?i)\\b[a-z]+(?:[\\s,.'-]+[a-z]+){7,}\\b");

    private final RestClient openRouterRestClient;
    private final OpponentAiProperties properties;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;

    public OpenRouterOpponentReportGenerator(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            OpponentAiProperties properties,
            PromptLoader promptLoader,
            ObjectMapper objectMapper
    ) {
        this.openRouterRestClient = openRouterRestClient;
        this.properties = properties;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public GeneratedOpponentReport generate(OpponentAnalysisInput input) {
        return generateWithMetadata(input).report();
    }

    @Override
    public OpponentReportGenerationResult generateWithMetadata(OpponentAnalysisInput input) {
        validateInput(input);
        validateConfiguration();

        OpponentAiException firstEmptyContentFailure = null;
        int outputTokens = configuredMaxOutputTokens();
        for (int attempt = 0; attempt < 2; attempt++) {
            OpenRouterChatCompletionResponse response = callWithRetry(input, outputTokens);
            try {
                String content = extractContent(response);
                GeneratedOpponentReport report = parseReport(content);
                validateReport(report);
                OpenRouterChatCompletionResponse.Usage usage = response.usage();
                return new OpponentReportGenerationResult(
                        properties.getProvider(),
                        properties.getModel(),
                        properties.getPromptVersion(),
                        report,
                        new OpponentReportGenerationResult.Usage(
                                usage == null ? null : usage.promptTokens(),
                                usage == null ? null : usage.completionTokens(),
                                usage == null ? null : usage.totalTokens()
                        ),
                        OffsetDateTime.now()
                );
            } catch (OpponentAiException exception) {
                if (!"AI_OUTPUT_TRUNCATED".equals(exception.getCode()) || attempt == 1) {
                    throw exception;
                }
                firstEmptyContentFailure = exception;
                outputTokens = retryOutputTokens(outputTokens);
            }
        }
        throw firstEmptyContentFailure;
    }

    private void validateConfiguration() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new OpponentAiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENROUTER_API_KEY_MISSING",
                    "OpenRouter api key must be configured"
            );
        }
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new OpponentAiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENROUTER_MODEL_MISSING",
                    "OpenRouter model must be configured"
            );
        }
    }

    private OpenRouterChatCompletionResponse callWithRetry(OpponentAnalysisInput input, int maxOutputTokens) {
        OpponentAiException firstFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return callOpenRouter(input, maxOutputTokens);
            } catch (OpponentAiException exception) {
                if (firstFailure == null) {
                    firstFailure = exception;
                }
                if (attempt == 1 || !retryable(exception)) {
                    throw exception;
                }
            }
        }
        throw firstFailure;
    }

    private OpenRouterChatCompletionResponse callOpenRouter(OpponentAnalysisInput input, int maxOutputTokens) {
        long startedAt = System.nanoTime();
        try {
            return openRouterRestClient.post()
                    .uri("/chat/completions")
                    .body(request(input, maxOutputTokens))
                    .retrieve()
                    .body(OpenRouterChatCompletionResponse.class);
        } catch (OpponentAiException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new OpponentAiException(
                    HttpStatus.BAD_REQUEST,
                    "AI_INVALID_INPUT",
                    "Opponent analysis input could not be serialized",
                    exception
            );
        } catch (RestClientResponseException exception) {
            Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
            OpponentAiException mapped = mapHttpError(exception);
            log.warn(
                    "OpenRouter request rejected, provider={}, model={}, status={}, durationMs={}, reason={}",
                    properties.getProvider(),
                    properties.getModel(),
                    exception.getStatusCode().value(),
                    duration.toMillis(),
                    mapped.getMessage()
            );
            throw mapped;
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
                log.warn(
                        "OpenRouter request timed out, provider={}, model={}, durationMs={}",
                        properties.getProvider(),
                        properties.getModel(),
                        duration.toMillis()
                );
                throw new OpponentAiException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "OPENROUTER_TIMEOUT",
                        "OpenRouter request timed out",
                        exception
                );
            }
            throw new OpponentAiException(
                    HttpStatus.BAD_GATEWAY,
                    "OPENROUTER_IO_ERROR",
                    "Failed to call OpenRouter",
                    exception
            );
        } catch (RestClientException exception) {
            throw new OpponentAiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_INVALID_RESPONSE",
                    "OpenRouter response could not be parsed",
                    exception
            );
        }
    }

    private GeneratedOpponentReport parseReport(String content) {
        try {
            return objectMapper.readValue(stripCodeFence(content), GeneratedOpponentReport.class);
        } catch (JsonProcessingException exception) {
            throw new OpponentAiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_INVALID_RESPONSE",
                    "OpenRouter returned invalid report JSON",
                    exception
            );
        }
    }

    private OpenRouterChatCompletionRequest request(OpponentAnalysisInput input, int maxOutputTokens) throws JsonProcessingException {
        return new OpenRouterChatCompletionRequest(
                properties.getModel(),
                List.of(
                        new OpenRouterChatCompletionRequest.Message(SYSTEM_ROLE, promptLoader.opponentAnalysisSystemPrompt()),
                        new OpenRouterChatCompletionRequest.Message(USER_ROLE, objectMapper.writeValueAsString(input))
                ),
                properties.getTemperature(),
                maxOutputTokens,
                reasoning()
        );
    }

    private OpenRouterChatCompletionRequest.Reasoning reasoning() {
        if (!properties.isReasoningEnabled()) {
            return null;
        }
        return OpenRouterChatCompletionRequest.Reasoning.disabled();
    }

    private OpponentAiException mapHttpError(RestClientResponseException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        String reason = extractOpenRouterReason(exception);
        if (status == HttpStatus.UNAUTHORIZED) {
            return new OpponentAiException(
                    HttpStatus.UNAUTHORIZED,
                    "OPENROUTER_AUTH_FAILED",
                    reason == null ? "OpenRouter authentication failed" : reason,
                    exception
            );
        }
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            return new OpponentAiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "AI_RATE_LIMITED",
                    reason == null ? "OpenRouter rate limit reached" : reason,
                    exception
            );
        }
        String code = status != null && status.is4xxClientError() ? "OPENROUTER_REQUEST_REJECTED" : "OPENROUTER_HTTP_ERROR";
        return new OpponentAiException(
                HttpStatus.BAD_GATEWAY,
                code,
                reason == null ? "OpenRouter returned an error response" : reason,
                exception
        );
    }

    private String extractOpenRouterReason(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            Map<?, ?> root = objectMapper.readValue(body, Map.class);
            Object error = root.get("error");
            if (error instanceof Map<?, ?> errorMap) {
                Object message = errorMap.get("message");
                if (message instanceof String text && !text.isBlank()) {
                    return text;
                }
            }
            Object rootMessage = root.get("message");
            if (rootMessage instanceof String text && !text.isBlank()) {
                return text;
            }
        } catch (JsonProcessingException ignored) {
            String trimmed = body.trim();
            return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
        }
        return null;
    }

    private String extractContent(OpenRouterChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new OpponentAiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_INVALID_RESPONSE",
                    "OpenRouter returned no choices"
            );
        }
        OpenRouterChatCompletionResponse.Choice choice = response.choices().get(0);
        OpenRouterChatCompletionResponse.Message message = choice.message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            logEmptyContent(response, choice, message);
            if ("length".equalsIgnoreCase(choice.finishReason())) {
                throw new OpponentAiException(
                        HttpStatus.BAD_GATEWAY,
                        "AI_OUTPUT_TRUNCATED",
                        "The model exhausted its output token budget before producing final content"
                );
            }
            if (hasReasoning(message)) {
                throw new OpponentAiException(
                        HttpStatus.BAD_GATEWAY,
                        "AI_FINAL_CONTENT_MISSING",
                        "The model returned reasoning but no final answer"
                );
            }
            throw new OpponentAiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_EMPTY_RESPONSE",
                    "OpenRouter returned empty message content"
            );
        }
        return message.content();
    }

    private void logEmptyContent(
            OpenRouterChatCompletionResponse response,
            OpenRouterChatCompletionResponse.Choice choice,
            OpenRouterChatCompletionResponse.Message message
    ) {
        OpenRouterChatCompletionResponse.Usage usage = response.usage();
        log.warn(
                "OpenRouter returned empty final content, responseId={}, model={}, provider={}, finishReason={}, nativeFinishReason={}, promptTokens={}, completionTokens={}, totalTokens={}, reasoningTokens={}, reasoningPresent={}, reasoningChars={}, contentEmpty={}",
                response.id(),
                response.model(),
                response.provider(),
                choice.finishReason(),
                choice.nativeFinishReason(),
                usage == null ? null : usage.promptTokens(),
                usage == null ? null : usage.completionTokens(),
                usage == null ? null : usage.totalTokens(),
                usage == null ? null : usage.reasoningTokens(),
                hasReasoning(message),
                reasoningChars(message),
                message == null || message.content() == null || message.content().isBlank()
        );
    }

    private boolean hasReasoning(OpenRouterChatCompletionResponse.Message message) {
        return message != null && message.reasoning() != null && !message.reasoning().isBlank();
    }

    private int reasoningChars(OpenRouterChatCompletionResponse.Message message) {
        if (message == null) {
            return 0;
        }
        return message.reasoning() == null ? 0 : message.reasoning().length();
    }

    private String stripCodeFence(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private void validateInput(OpponentAnalysisInput input) {
        if (input == null || input.match() == null || input.ourTeam() == null || input.opponent() == null) {
            throw new OpponentAiException(
                    HttpStatus.BAD_REQUEST,
                    "AI_INVALID_INPUT",
                    "Opponent analysis input must include match, ourTeam and opponent"
            );
        }
    }

    private void validateReport(GeneratedOpponentReport report) {
        if (report == null) {
            invalidReport("AI report body is required");
        }
        if (report.threatLevel() == null) {
            invalidReport("AI report threatLevel is required");
        }
        if (report.summary() == null || report.summary().isBlank()) {
            invalidReport("AI report summary is required");
        }
        if (report.summary().length() > 1200) {
            invalidReport("AI report summary is too long");
        }
        validateFindings(report.strengths(), "strengths");
        validateFindings(report.weaknesses(), "weaknesses");
        String text = String.join(" ",
                report.summary(),
                report.recommendations().toString(),
                report.dataLimitations().toString(),
                report.comparison().toString(),
                report.strengths().toString(),
                report.weaknesses().toString()
        );
        if (LONG_ENGLISH_SENTENCE.matcher(text).find()) {
            invalidReport("AI report contains excessive English prose");
        }
    }

    private void validateFindings(List<AnalysisFinding> findings, String fieldName) {
        for (AnalysisFinding finding : findings) {
            if (finding == null || finding.title() == null || finding.title().isBlank()
                    || finding.analysis() == null || finding.analysis().isBlank()) {
                invalidReport("AI report " + fieldName + " items must include title and analysis");
            }
        }
    }

    private void invalidReport(String message) {
        throw new OpponentAiException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE", message);
    }

    private boolean retryable(OpponentAiException exception) {
        return "AI_RATE_LIMITED".equals(exception.getCode())
                || "OPENROUTER_HTTP_ERROR".equals(exception.getCode())
                || "OPENROUTER_TIMEOUT".equals(exception.getCode());
    }

    private int configuredMaxOutputTokens() {
        Integer configured = properties.getMaxOutputTokens();
        if (configured == null || configured < 1) {
            return 4000;
        }
        return configured;
    }

    private int retryOutputTokens(int current) {
        return Math.max(current, Math.min(current + 1000, MAX_RETRY_OUTPUT_TOKENS));
    }

    private boolean isTimeout(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
