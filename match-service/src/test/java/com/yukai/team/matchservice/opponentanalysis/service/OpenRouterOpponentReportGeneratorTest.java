package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.opponentanalysis.config.OpponentAiProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.GeneratedOpponentReport;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentAnalysisInput;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.ThreatLevel;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class OpenRouterOpponentReportGeneratorTest {

    @Test
    void callsOpenRouterAndParsesStructuredReportWithUsage() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.max_tokens").value(8192))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.reasoning").doesNotExist())
                .andExpect(content().string(not(containsString("response_format"))))
                .andExpect(content().string(not(containsString("structured_outputs"))))
                .andExpect(content().string(not(containsString("json_schema"))))
                .andRespond(withSuccess(openRouterResponse(reportJson("MEDIUM", "对手有一定威胁。"), 1, 2, 3), MediaType.APPLICATION_JSON));

        var result = generator(builder.build(), configuredProperties()).generateWithMetadata(input());

        assertThat(result.provider()).isEqualTo("openrouter");
        assertThat(result.model()).isEqualTo("test-model");
        assertThat(result.promptVersion()).isEqualTo("v1");
        assertThat(result.report().threatLevel()).isEqualTo(ThreatLevel.MEDIUM);
        assertThat(result.report().summary()).isEqualTo("对手有一定威胁。");
        assertThat(result.usage().promptTokens()).isEqualTo(1);
        assertThat(result.usage().completionTokens()).isEqualTo(2);
        assertThat(result.usage().totalTokens()).isEqualTo(3);
        server.verify();
    }

    @Test
    void generateReturnsReportOnly() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(openRouterResponse(reportJson("LOW", "测试。"), null, null, null), MediaType.APPLICATION_JSON));

        var report = generator(builder.build(), configuredProperties()).generate(input());

        assertThat(report.threatLevel()).isEqualTo(ThreatLevel.LOW);
        server.verify();
    }

    @Test
    void stripsJsonCodeFenceBeforeParsing() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(openRouterResponse("```json\n" + reportJson("LOW", "测试。") + "\n```", null, null, null), MediaType.APPLICATION_JSON));

        var report = generator(builder.build(), configuredProperties()).generate(input());

        assertThat(report.threatLevel()).isEqualTo(ThreatLevel.LOW);
        server.verify();
    }

    @Test
    void failsWithInvalidResponseForTruncatedJsonWhenFinishReasonIsStopAndLogsDiagnostics(CapturedOutput output) {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(openRouterResponse("{\n\"threatLevel\":", "stop", 101, 8192, 8293), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE"))
                .hasMessage("OpenRouter returned invalid report JSON");
        assertThat(output.getOut())
                .contains("OpenRouter response received")
                .contains("finishReason=stop")
                .contains("rawContentLength=16")
                .contains("normalizedContentLength=16")
                .contains("promptTokens=101")
                .contains("completionTokens=8192")
                .contains("totalTokens=8293")
                .contains("maxTokensRequestField=max_tokens")
                .contains("configuredOutputTokenLimit=8192")
                .contains("outputTokenLimit=8192")
                .contains("OpenRouter returned invalid report JSON")
                .contains("exceptionClass=com.fasterxml.jackson.core.io.JsonEOFException")
                .contains("normalizedContentPrefix={");
        server.verify();
    }

    @Test
    void mapsLengthFinishReasonWithNonEmptyContentToTruncatedWithoutParsing(CapturedOutput output) {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(jsonPath("$.max_tokens").value(8192))
                .andRespond(withSuccess(openRouterResponse("not-json", "length", 101, 8192, 8293), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(jsonPath("$.max_tokens").value(8192))
                .andRespond(withSuccess(openRouterResponse("not-json", "length", 102, 8192, 8294), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_OUTPUT_TRUNCATED"))
                .hasMessage("AI response truncated because output token limit was reached");
        assertThat(output.getOut())
                .contains("finishReason=length")
                .contains("completionTokens=8192")
                .contains("outputTokenLimit=8192")
                .contains("OpenRouter response truncated due to output token limit")
                .contains("totalTokens=8294")
                .doesNotContain("Unrecognized token 'not'");
        server.verify();
    }

    @Test
    void generatedOpponentReportSchemaRemainsUnchanged() {
        List<String> componentNames = Arrays.stream(GeneratedOpponentReport.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(componentNames).containsExactly(
                "threatLevel",
                "summary",
                "comparison",
                "strengths",
                "weaknesses",
                "recommendations",
                "dataLimitations"
        );
    }

    @Test
    void normalizationDoesNotTruncateValidRawJsonWithWhitespace(CapturedOutput output) {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String json = reportJson("LOW", "测试。");
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(openRouterResponse("\n  " + json + "  \n", "stop", 10, 20, 30), MediaType.APPLICATION_JSON));

        var report = generator(builder.build(), configuredProperties()).generate(input());

        assertThat(report.threatLevel()).isEqualTo(ThreatLevel.LOW);
        assertThat(output.getOut())
                .contains("rawContentLength=" + (json.length() + 6))
                .contains("normalizedContentLength=" + json.length())
                .contains("finishReason=stop");
        server.verify();
    }

    @Test
    void diagnosticPreviewIsBoundedAndIncludesSuffixOnlyForLongContent(CapturedOutput output) {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String longInvalidJson = "{\"summary\":\"" + "a".repeat(2500) + "\",\"tail\":\"" + "z".repeat(2500);
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(openRouterResponse(longInvalidJson, "stop", 1, 2, 3), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE"));
        assertThat(output.getOut())
                .contains("contentPrefix=")
                .contains("contentSuffix=")
                .contains("normalizedContentPrefix=")
                .contains("normalizedContentSuffix=");
        server.verify();
    }

    @Test
    void failsWhenApiKeyIsMissing() {
        OpponentAiProperties properties = configuredProperties();
        properties.setApiKey("");

        assertThatThrownBy(() -> generator(builder().build(), properties).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.SERVICE_UNAVAILABLE, "OPENROUTER_API_KEY_MISSING"));
    }

    @Test
    void failsWhenModelIsMissing() {
        OpponentAiProperties properties = configuredProperties();
        properties.setModel("");

        assertThatThrownBy(() -> generator(builder().build(), properties).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.SERVICE_UNAVAILABLE, "OPENROUTER_MODEL_MISSING"));
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> generator(builder().build(), configuredProperties()).generate(null))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_REQUEST, "AI_INVALID_INPUT"));
    }

    @Test
    void mapsUnauthorizedTo401WithoutRetry() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.UNAUTHORIZED, "OPENROUTER_AUTH_FAILED"));
        server.verify();
    }

    @Test
    void mapsStructuredOutputsUnsupported400WithOpenRouterReason() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(content().string(not(containsString("response_format"))))
                .andExpect(content().string(not(containsString("structured_outputs"))))
                .andExpect(content().string(not(containsString("json_schema"))))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"message":"model does not support structured-outputs"}}
                                """));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> {
                    assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "OPENROUTER_REQUEST_REJECTED");
                    assertThat(ex).hasMessageContaining("model does not support structured-outputs");
                });
        server.verify();
    }

    @Test
    void mapsRateLimitTo429AndRetriesOnce() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(twice(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMITED"));
        server.verify();
    }

    @Test
    void mapsServerErrorToBadGatewayAndRetriesOnce() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(twice(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "OPENROUTER_HTTP_ERROR"));
        server.verify();
    }

    @Test
    void retriesOnceThenSucceeds() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(openRouterResponse(reportJson("HIGH", "对手明显强于我方。"), 5, 6, 11), MediaType.APPLICATION_JSON));

        var report = generator(builder.build(), configuredProperties()).generate(input());

        assertThat(report.threatLevel()).isEqualTo(ThreatLevel.HIGH);
        server.verify();
    }

    @Test
    void doesNotRetryInvalidJson() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"not-json"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE"));
        server.verify();
    }

    @Test
    void rejectsEmptyChoices() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE"));
        server.verify();
    }

    @Test
    void rejectsEmptyContent() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"\"}}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE"));
        server.verify();
    }

    @Test
    void rejectsInvalidThreatLevel() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(openRouterResponse(reportJson("CRITICAL", "测试。"), null, null, null), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE"));
        server.verify();
    }

    @Test
    void rejectsExcessiveEnglishOutput() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(openRouterResponse(reportJson("LOW", "This is a long English sentence that should not be accepted by validation."), null, null, null), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE"));
        server.verify();
    }

    @Test
    void mapsTimeoutToGatewayTimeout() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://openrouter.test")
                .requestFactory((uri, method) -> {
                    throw new SocketTimeoutException("timeout");
                })
                .build();

        assertThatThrownBy(() -> generator(restClient, configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.GATEWAY_TIMEOUT, "OPENROUTER_TIMEOUT"));
    }

    @Test
    void mapsLengthWithReasoningAndEmptyContentToOutputTruncatedAfterRetry() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(twice(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(emptyContentResponse("length", "native-length", "内部推理内容", null), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_OUTPUT_TRUNCATED"))
                .hasMessage("AI response truncated because output token limit was reached");
        server.verify();
    }

    @Test
    void mapsStopWithReasoningAndEmptyContentToFinalContentMissing() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(emptyContentResponse("stop", "stop", "内部推理内容", null), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_FINAL_CONTENT_MISSING"))
                .hasMessage("The model returned reasoning but no final answer");
        server.verify();
    }

    @Test
    void mapsEmptyContentAndEmptyReasoningToEmptyResponse() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(emptyContentResponse("stop", "stop", null, null), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE"));
        server.verify();
    }

    @Test
    void ignoresReasoningDetailsAndStillReachesContentExtraction() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(
                        emptyContentResponse("stop", "stop", null, "[{\"type\":\"reasoning.text\",\"text\":\"ignored\"}]"),
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE"));
        server.verify();
    }

    @Test
    void retriesOnceAfterLengthThenParsesSecondResponse() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(jsonPath("$.max_tokens").value(8192))
                .andRespond(withSuccess(emptyContentResponse("length", "length", "内部推理内容", null), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(jsonPath("$.max_tokens").value(8192))
                .andRespond(withSuccess(openRouterResponse(reportJson("HIGH", "第二次生成成功。"), 10, 20, 30), MediaType.APPLICATION_JSON));

        var report = generator(builder.build(), configuredProperties()).generate(input());

        assertThat(report.summary()).isEqualTo("第二次生成成功。");
        server.verify();
    }

    @Test
    void failsClearlyWhenSecondLengthResponseIsStillEmpty() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(jsonPath("$.max_tokens").value(8192))
                .andRespond(withSuccess(emptyContentResponse("length", "length", "内部推理内容", null), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(jsonPath("$.max_tokens").value(8192))
                .andRespond(withSuccess(emptyContentResponse("length", "length", "内部推理内容", null), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .satisfies(ex -> assertOpponentAiException(ex, HttpStatus.BAD_GATEWAY, "AI_OUTPUT_TRUNCATED"));
        server.verify();
    }

    @Test
    void doesNotExposeReasoningTextInExceptionOrLogs(CapturedOutput output) {
        String secretReasoning = "不要记录这段完整推理内容";
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andRespond(withSuccess(emptyContentResponse("stop", "stop", secretReasoning, null), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(builder.build(), configuredProperties()).generate(input()))
                .isInstanceOf(OpponentAiException.class)
                .hasMessageNotContaining(secretReasoning);
        assertThat(output.getOut()).doesNotContain(secretReasoning);
        assertThat(output.getErr()).doesNotContain(secretReasoning);
        server.verify();
    }

    @Test
    void sendsReasoningParameterOnlyWhenConfigured() {
        OpponentAiProperties properties = configuredProperties();
        properties.setReasoningEnabled(true);
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(jsonPath("$.reasoning.enabled").value(false))
                .andExpect(jsonPath("$.reasoning.effort").value("none"))
                .andExpect(jsonPath("$.reasoning.exclude").value(true))
                .andRespond(withSuccess(openRouterResponse(reportJson("LOW", "测试。"), null, null, null), MediaType.APPLICATION_JSON));

        generator(builder.build(), properties).generate(input());

        server.verify();
    }

    @Test
    void readsMaxOutputTokensFromConfiguration() {
        OpponentAiProperties properties = configuredProperties();
        properties.setMaxOutputTokens(3200);
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://openrouter.test/chat/completions"))
                .andExpect(jsonPath("$.max_tokens").value(3200))
                .andRespond(withSuccess(openRouterResponse(reportJson("LOW", "测试。"), null, null, null), MediaType.APPLICATION_JSON));

        generator(builder.build(), properties).generate(input());

        server.verify();
    }

    private RestClient.Builder builder() {
        return RestClient.builder()
                .baseUrl("https://openrouter.test")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
    }

    private OpenRouterOpponentReportGenerator generator(RestClient restClient, OpponentAiProperties properties) {
        return new OpenRouterOpponentReportGenerator(
                restClient,
                properties,
                new PromptLoader(),
                new JacksonConfig().objectMapper()
        );
    }

    private OpponentAiProperties configuredProperties() {
        OpponentAiProperties properties = new OpponentAiProperties();
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        return properties;
    }

    private OpponentAnalysisInput input() {
        return new OpponentAnalysisInput(
                new OpponentAnalysisInput.MatchContext("Test League", "2026", OffsetDateTime.parse("2026-08-10T18:30:00Z"), HomeAway.HOME),
                team("Our Team"),
                team("Opponent Team"),
                new OpponentAnalysisInput.LeagueAverage(new BigDecimal("2.58"), new BigDecimal("2.58"), new BigDecimal("1.35"), new BigDecimal("0.42")),
                new OpponentAnalysisInput.ComparisonMetrics(
                        1,
                        -1,
                        new BigDecimal("0.10"),
                        new BigDecimal("0.20"),
                        new BigDecimal("0.30"),
                        new BigDecimal("-0.10"),
                        3,
                        1,
                        new BigDecimal("0.40"),
                        new BigDecimal("0.50")
                ),
                List.of("固定测试数据")
        );
    }

    private OpponentAnalysisInput.TeamMetrics team(String name) {
        return new OpponentAnalysisInput.TeamMetrics(
                name,
                1,
                30,
                15,
                9,
                3,
                3,
                28,
                18,
                new BigDecimal("1.87"),
                new BigDecimal("1.20"),
                new BigDecimal("0.60"),
                List.of("won", "draw", "lost")
        );
    }

    private String openRouterResponse(String content, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        return openRouterResponse(content, "stop", promptTokens, completionTokens, totalTokens);
    }

    private String openRouterResponse(String content, String finishReason, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        String usage = promptTokens == null ? "null" : """
                {"prompt_tokens": %d, "completion_tokens": %d, "total_tokens": %d}
                """.formatted(promptTokens, completionTokens, totalTokens);
        return """
                {
                  "id": "chatcmpl-test",
                  "model": "test-model",
                  "provider": "Novita",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "%s"
                    },
                    "finish_reason": "%s"
                  }],
                  "usage": %s
                }
                """.formatted(escape(content), finishReason, usage);
    }

    private String reportJson(String threatLevel, String summary) {
        return """
                {"threatLevel":"%s","summary":"%s","comparison":[],"strengths":[{"title":"进攻效率","analysis":"对手场均进球更高。","evidence":["opponent.goalsForPerGame=4.31"]}],"weaknesses":[],"recommendations":["压缩中路空间"],"dataLimitations":["固定测试输入不包含球员数据。"]}
                """.formatted(threatLevel, summary).trim();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String emptyContentResponse(String finishReason, String nativeFinishReason, String reasoning, String reasoningDetails) {
        String reasoningField = reasoning == null ? "" : ", \"reasoning\": \"" + escape(reasoning) + "\"";
        String detailsField = reasoningDetails == null ? "" : ", \"reasoning_details\": " + reasoningDetails;
        return """
                {
                  "id": "chatcmpl-empty",
                  "model": "test-model",
                  "provider": "Novita",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "%s",
                    "native_finish_reason": "%s",
                    "message": {
                      "role": "assistant",
                      "content": ""%s%s
                    }
                  }],
                  "usage": {
                    "prompt_tokens": 100,
                    "completion_tokens": 8192,
                    "total_tokens": 8292,
                    "reasoning_tokens": 3990
                  }
                }
                """.formatted(finishReason, nativeFinishReason, reasoningField, detailsField);
    }

    private void assertOpponentAiException(Throwable throwable, HttpStatus status, String code) {
        OpponentAiException exception = (OpponentAiException) throwable;
        assertThat(exception.getHttpStatus()).isEqualTo(status);
        assertThat(exception.getCode()).isEqualTo(code);
    }
}
