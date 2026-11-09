package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

class MetricsInputFingerprintTest {
    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new JacksonConfig().objectMapper();
    private final MetricsInputFingerprint fingerprints = new MetricsInputFingerprint(mapper);
    private final OpponentAnalysisInputMapper inputs = new OpponentAnalysisInputMapper();

    @Test
    void sameInputAndReorderedObjectKeysAreStable() throws Exception {
        var input = inputs.toInput(RecentFormAiFixture.metrics());
        assertThat(fingerprints.fingerprint(input)).hasSize(64).isEqualTo(fingerprints.fingerprint(input));
        assertThat(fingerprints.fingerprint(mapper.readTree("{\"b\":[{\"y\":2,\"x\":1}],\"a\":null}")))
                .isEqualTo(fingerprints.fingerprint(mapper.readTree("{\"a\":null,\"b\":[{\"x\":1,\"y\":2}]}")));
    }

    @Test
    void fetchAndCacheMetadataNeverReachInputOrInvalidateIt() {
        var metrics = RecentFormAiFixture.metrics();
        ObjectNode changed = mapper.valueToTree(metrics);
        for (String field : new String[]{"dataSource", "ourTeam/recentForm/source", "opponent/recentForm/source"}) {
            var node = (ObjectNode) changed.at("/" + field);
            node.put("fetchedAt", "2026-11-09T10:00:00Z").put("cacheStatus", "MISS")
                    .put("cacheTtlSeconds", 123).put("forceRefreshRequested", true);
        }
        assertThat(fingerprints.fingerprint(inputs.toInput(mapper.convertValue(changed, MatchOpponentMetricsResponse.class))))
                .isEqualTo(fingerprints.fingerprint(inputs.toInput(metrics)));
    }

    @Test
    void numericScaleAndEquivalentKickoffOffsetsAreStable() throws Exception {
        assertThat(fingerprints.fingerprint(mapper.readTree("{\"rate\":0.80}")))
                .isEqualTo(fingerprints.fingerprint(mapper.readTree("{\"rate\":0.8}")));
        ObjectNode metrics = mapper.valueToTree(RecentFormAiFixture.metrics());
        String original = fingerprints.fingerprint(inputs.toInput(RecentFormAiFixture.metrics()));
        ((ObjectNode) metrics.get("match")).put("matchTime", "2026-08-10T20:30:00+02:00");
        assertThat(fingerprints.fingerprint(inputs.toInput(mapper.convertValue(metrics, MatchOpponentMetricsResponse.class))))
                .isEqualTo(original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"score", "results", "availability", "time", "identity", "rules", "contract", "standings", "recentVersion"})
    void meaningfulInputChangesInvalidateFingerprint(String change) {
        ObjectNode input = mapper.valueToTree(inputs.toInput(RecentFormAiFixture.metrics()));
        String original = fingerprints.fingerprint(input);
        switch (change) {
            case "score" -> ((ObjectNode) input.at("/ourTeam/recentForm/recentFive")).put("goalsFor", 28);
            case "results" -> ((ObjectNode) input.get("ourTeam")).set("recentFive", mapper.valueToTree(java.util.List.of("won", "won", "won", "won", "lost")));
            case "availability" -> ((ObjectNode) input.at("/ourTeam/recentForm")).put("recentFormAvailable", false).put("recentFormStatus", "SOURCE_UNAVAILABLE");
            case "time" -> ((ObjectNode) input.get("match")).put("matchTime", "2026-08-12T18:30:00Z");
            case "identity" -> { var ours = input.get("ourTeam"); input.set("ourTeam", input.get("opponent")); input.set("opponent", ours); }
            case "rules" -> ((ObjectNode) input.get("provenance")).put("recentFormRulesVersion", "vNext");
            case "contract" -> ((ObjectNode) input.get("provenance")).put("contractVersion", "vNext");
            case "standings" -> ((ObjectNode) input.get("provenance")).put("standingsSnapshotId", 99);
            case "recentVersion" -> ((ObjectNode) input.at("/opponent/recentForm/source")).put("versionId", "new-scores");
        }
        assertThat(fingerprints.fingerprint(input)).isNotEqualTo(original);
    }

    @Test
    void unavailableReasonsRemainDistinctAndRecoveryChangesFingerprint() {
        ObjectNode metrics = mapper.valueToTree(RecentFormAiFixture.metrics());
        String available = fingerprints.fingerprint(inputs.toInput(RecentFormAiFixture.metrics()));
        ((ObjectNode) metrics.at("/ourTeam/recentForm")).put("status", "SOURCE_UNAVAILABLE");
        String unavailable = fingerprints.fingerprint(inputs.toInput(mapper.convertValue(metrics, MatchOpponentMetricsResponse.class)));
        ((ObjectNode) metrics.at("/ourTeam/recentForm")).put("status", "NO_COMPLETED_MATCHES");
        String noMatches = fingerprints.fingerprint(inputs.toInput(mapper.convertValue(metrics, MatchOpponentMetricsResponse.class)));
        assertThat(unavailable).isNotEqualTo(available).isNotEqualTo(noMatches);
    }
}
