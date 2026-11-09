package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchesPageResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMapping;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.parser.FlaTeamMatchesHtmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlaRecentFormServiceTest {
    @Mock private FlaTeamMatchesSnapshotService snapshots;
    private FlaProperties properties;
    private FlaRecentFormService service;

    @BeforeEach
    void setUp() {
        properties = new FlaProperties();
        service = new FlaRecentFormService(snapshots, new RecentFormCalculator(), properties);
    }

    @Test
    void realFixturesWithUnknownSeasonAreAvailableWithoutLeagueConfiguration() throws IOException {
        for (long id : new long[]{270, 63}) {
            var page = new FlaTeamMatchesHtmlParser().parsePage(new ClassPathResource("fla/team-" + id + ".html")
                    .getContentAsString(StandardCharsets.UTF_8), id);
            assertThat(page.championnatId()).isEqualTo(16L);
            assertThat(page.saisonId()).isNull();
            stub(page, false);
            var form = service.getForm(mapping(id), RecentFormCalculatorTest.TARGET, false);
            assertThat(form.status()).isEqualTo(RecentFormMetrics.Status.AVAILABLE);
            assertThat(form.available()).isTrue();
            assertThat(form.sampleSize()).isEqualTo(id == 270 ? 6 : 7);
            assertThat(form.source().sourceSaisonId()).isNull();
            assertThat(form.competitionScope()).isEqualTo(RecentFormMetrics.CompetitionScope.ALL_COMPETITIONS);
            assertThat(form.seasonScope()).isEqualTo(RecentFormMetrics.SeasonScope.CROSS_SEASON_ALLOWED);
            assertThat(form.results().stream().limit(5)).containsExactlyElementsOf(id == 270
                    ? java.util.List.of("lost", "won", "won", "won", "won")
                    : java.util.List.of("won", "won", "draw", "lost", "lost"));
            assertThat(form.source().versionId()).isEqualTo("version");
        }
    }

    @Test
    void forceRefreshPropagatesWithoutRequiringSeasonValidation() throws IOException {
        stub(new FlaTeamMatchesPageResponse(270L, 16L, 2L, RecentFormCalculatorTest.fixture(270)), true);
        var result = service.getForm(mapping(270), RecentFormCalculatorTest.TARGET, true);
        assertThat(result.available()).isTrue();
        assertThat(result.sampleSize()).isEqualTo(6);
        assertThat(result.source().forceRefreshRequested()).isTrue();
        verify(snapshots).getTeamMatches(270L, true);
    }

    @ParameterizedTest
    @CsvSource(value = {"1, 16", "NULL, NULL", "99, 17"}, nullValues = "NULL")
    void seasonAndChampionshipIdentityDoNotRestrictOfficialRecentMatches(Long sourceSeason, Long sourceChampionship) throws IOException {
        stub(new FlaTeamMatchesPageResponse(270L, sourceChampionship, sourceSeason, RecentFormCalculatorTest.fixture(270)), false);
        var result = service.getForm(mapping(270), RecentFormCalculatorTest.TARGET, false);
        assertThat(result.available()).isTrue();
        assertThat(result.sampleSize()).isEqualTo(6);
        assertThat(result.source().sourceSaisonId()).isEqualTo(sourceSeason);
    }

    @Test
    void wrongTeamIdentityStillMakesSourceUnavailable() {
        when(snapshots.getTeamMatches(270L, false)).thenReturn(new FlaTeamMatchesSnapshotService.Result(
                new FlaTeamMatchesPageResponse(63L, 16L, null, java.util.List.of()), "version",
                RecentFormCalculatorTest.FETCHED, CacheStatus.HIT, 21600, false));
        var form = service.getForm(mapping(270), RecentFormCalculatorTest.TARGET, false);
        assertThat(form.status()).isEqualTo(RecentFormMetrics.Status.SOURCE_UNAVAILABLE);
        assertThat(form.available()).isFalse();
        assertThat(form.sampleSize()).isNull();
        assertThat(form.source().errorCode()).isEqualTo("FLA_INVALID_RESPONSE");
    }

    @ParameterizedTest
    @CsvSource({"FLA_TIMEOUT, GATEWAY_TIMEOUT", "FLA_UNAVAILABLE, BAD_GATEWAY", "FLA_INVALID_RESPONSE, BAD_GATEWAY"})
    void requestAndParserFailuresRetainTheirErrorCode(String code, HttpStatus status) {
        when(snapshots.getTeamMatches(270L, false)).thenThrow(new FlaClientException(status, code, "safe"));
        var form = service.getForm(mapping(270), RecentFormCalculatorTest.TARGET, false);
        assertThat(form.status()).isEqualTo(RecentFormMetrics.Status.SOURCE_UNAVAILABLE);
        assertThat(form.sampleSize()).isNull();
        assertThat(form.source().errorCode()).isEqualTo(code);
        assertThat(form.source().versionId()).isNull();
    }

    @Test
    void knownEmptySampleDiffersFromUnverifiedOrUnavailableSource() {
        stub(new FlaTeamMatchesPageResponse(270L, 16L, 2L, java.util.List.of()), false);
        var form = service.getForm(mapping(270), RecentFormCalculatorTest.TARGET, false);
        assertThat(form.status()).isEqualTo(RecentFormMetrics.Status.NO_COMPLETED_MATCHES);
        assertThat(form.sampleSize()).isZero();
        assertThat(form.recentFive().winRate()).isNull();
    }

    static FlaTeamMapping mapping(long id) {
        FlaTeamMapping mapping = new FlaTeamMapping();
        mapping.setFlaTeamId(id);
        mapping.setFlaChampionnatId(16L);
        mapping.setFlaSaisonId(2L);
        return mapping;
    }

    private void stub(FlaTeamMatchesPageResponse page, boolean refresh) {
        when(snapshots.getTeamMatches(page.flaTeamId(), refresh)).thenReturn(new FlaTeamMatchesSnapshotService.Result(
                page, "version", RecentFormCalculatorTest.FETCHED, CacheStatus.MISS, 21600, refresh));
    }
}
