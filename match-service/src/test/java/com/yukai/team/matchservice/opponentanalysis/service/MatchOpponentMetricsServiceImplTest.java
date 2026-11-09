package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMapping;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAnalysisConflictException;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamMappingRepository;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchOpponentMetricsServiceImplTest {

    @Mock
    private MatchInfoRepository matchInfoRepository;
    @Mock
    private FlaTeamMappingRepository flaTeamMappingRepository;
    @Mock
    private FlaStandingsSnapshotService flaStandingsSnapshotService;

    @Mock
    private FlaRecentFormService recentFormService;

    @org.junit.jupiter.api.BeforeEach
    void recentSourceIsExplicitlyUnavailableByDefault() {
        org.mockito.Mockito.lenient().when(recentFormService.getForm(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics.unavailable(
                        com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics.Status.SOURCE_UNAVAILABLE, null));
    }

    @Test
    void returnsMetrics() {
        MatchInfo match = match();
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.of(mapping(12L, 6580L, 1365L, 15L)));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 13L))
                .thenReturn(Optional.of(mapping(13L, 6581L, 1365L, 15L)));
        when(flaStandingsSnapshotService.getStandings(1365L, 15L, false))
                .thenReturn(snapshotResult(
                        entry(6580L, "Our FLA", 20, 6, 2, 2, 30, 10),
                        entry(6581L, "Opponent FLA", 10, 3, 1, 6, 10, 20)
                ));

        var response = service().getMetrics(42L, false);

        assertThat(response.match().matchId()).isEqualTo(42L);
        assertThat(response.match().ourTeamName()).isEqualTo("Our Team");
        assertThat(response.ourTeam().internalTeamId()).isEqualTo(12L);
        assertThat(response.ourTeam().flaTeamId()).isEqualTo(6580L);
        assertThat(response.ourTeam().calculatedRank()).isEqualTo(1);
        assertThat(response.opponent().calculatedRank()).isEqualTo(2);
        assertThat(response.league().teamCount()).isEqualTo(2);
        assertThat(response.comparison().pointsDifference()).isEqualTo(10);
        assertThat(response.dataSource().provider()).isEqualTo("FLA");
        assertThat(response.dataSource().championnatId()).isEqualTo(1365L);
        assertThat(response.dataSource().saisonId()).isEqualTo(15L);
        assertThat(response.dataSource().snapshotId()).isEqualTo(10L);
        assertThat(response.dataSource().payloadHash()).isEqualTo(payloadHash());
        assertThat(response.dataSource().cacheStatus()).isEqualTo(CacheStatus.HIT);
        assertThat(response.dataSource().cacheTtlSeconds()).isEqualTo(21600L);
        assertThat(response.dataSource().forceRefreshRequested()).isFalse();
        assertThat(response.dataSource().formOrder()).isEqualTo("LATEST_TO_OLDEST");
        assertThat(response.dataSource().rankingType()).isEqualTo("CALCULATED");
        assertThat(response.ourTeam().recentFive()).isEmpty();
        assertThat(response.ourTeam().recentFiveWins()).isNull();
        assertThat(response.ourTeam().recentFormAvailable()).isFalse();
        assertThat(response.comparison().recentFiveWinsDifference()).isNull();
    }

    @Test
    void passesForceRefreshToSnapshotService() {
        commonMappings();
        when(flaStandingsSnapshotService.getStandings(1365L, 15L, true))
                .thenReturn(snapshotResult(
                        entry(6580L, "Our FLA", 20, 6, 2, 2, 30, 10),
                        entry(6581L, "Opponent FLA", 10, 3, 1, 6, 10, 20)
                ));

        var response = service().getMetrics(42L, true);

        assertThat(response.dataSource().forceRefreshRequested()).isTrue();
        verify(flaStandingsSnapshotService).getStandings(1365L, 15L, true);
        verify(recentFormService, org.mockito.Mockito.times(2)).getForm(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    void failsWhenMatchDoesNotExist() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Match not found");
    }

    @Test
    void failsWhenOurMappingIsMissing() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match()));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("Our team FLA mapping is missing");
    }

    @Test
    void failsWhenOpponentMappingIsMissing() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match()));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.of(mapping(12L, 6580L, 1365L, 15L)));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 13L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("Opponent team FLA mapping is missing");
    }

    @Test
    void failsWhenChampionnatDiffers() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match()));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.of(mapping(12L, 6580L, 1365L, 15L)));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 13L))
                .thenReturn(Optional.of(mapping(13L, 6581L, 1366L, 15L)));

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("Our team and opponent FLA championnat do not match");
    }

    @Test
    void failsWhenSaisonDiffers() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match()));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.of(mapping(12L, 6580L, 1365L, 15L)));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 13L))
                .thenReturn(Optional.of(mapping(13L, 6581L, 1365L, 16L)));

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("Our team and opponent FLA saison do not match");
    }

    @Test
    void failsWhenFlaStandingsMissOurTeam() {
        commonMappings();
        when(flaStandingsSnapshotService.getStandings(1365L, 15L, false))
                .thenReturn(snapshotResult(entry(6581L, "Opponent FLA", 10, 3, 1, 6, 10, 20)));

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("FLA standings do not contain our mapped team");
    }

    @Test
    void failsWhenFlaStandingsMissOpponentTeam() {
        commonMappings();
        when(flaStandingsSnapshotService.getStandings(1365L, 15L, false))
                .thenReturn(snapshotResult(entry(6580L, "Our FLA", 20, 6, 2, 2, 30, 10)));

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("FLA standings do not contain opponent mapped team");
    }

    @Test
    void failsWhenFlaStandingsAreEmpty() {
        commonMappings();
        when(flaStandingsSnapshotService.getStandings(1365L, 15L, false)).thenReturn(snapshotResult());

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("FLA standings do not contain our mapped team");
    }

    @Test
    void propagatesFlaClientException() {
        commonMappings();
        when(flaStandingsSnapshotService.getStandings(1365L, 15L, false))
                .thenThrow(new FlaClientException(HttpStatus.BAD_GATEWAY, "FLA_SERVICE_UNAVAILABLE", "FLA service unavailable"));

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(FlaClientException.class)
                .hasMessage("FLA service unavailable");
    }

    @Test
    void mergesOfficialFixtureMatchesAndComparesBothTeams() throws java.io.IOException {
        fixtureMappings();
        var calculator = new RecentFormCalculator();
        var ourForm = calculator.calculate(270L, RecentFormCalculatorTest.fixture(270),
                RecentFormCalculatorTest.TARGET, new com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics.Source(
                        270L, null, "a", "a", RecentFormCalculatorTest.FETCHED, CacheStatus.HIT, 21600, false, null));
        var theirForm = calculator.calculate(63L, RecentFormCalculatorTest.fixture(63),
                RecentFormCalculatorTest.TARGET, new com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics.Source(
                        63L, null, "b", "b", RecentFormCalculatorTest.FETCHED, CacheStatus.MISS, 21600, false, null));
        when(recentFormService.getForm(org.mockito.ArgumentMatchers.argThat(m -> m != null && m.getFlaTeamId().equals(270L)),
                org.mockito.ArgumentMatchers.eq(RecentFormCalculatorTest.TARGET), org.mockito.ArgumentMatchers.eq(false))).thenReturn(ourForm);
        when(recentFormService.getForm(org.mockito.ArgumentMatchers.argThat(m -> m != null && m.getFlaTeamId().equals(63L)),
                org.mockito.ArgumentMatchers.eq(RecentFormCalculatorTest.TARGET), org.mockito.ArgumentMatchers.eq(false))).thenReturn(theirForm);
        var result = service().getMetrics(42L);
        assertThat(result.ourTeam().recentFive()).containsExactly("lost", "won", "won", "won", "won");
        assertThat(result.opponent().recentFive()).containsExactly("won", "won", "draw", "lost", "lost");
        assertThat(result.opponent().recentFormSampleSize()).isEqualTo(7);
        assertThat(result.comparison().recentFiveWinsDifference()).isEqualTo(2);
        assertThat(result.ourTeam().recentForm().source().cacheStatus()).isEqualTo(CacheStatus.HIT);
    }

    @Test
    void realPagesDoNotConsumeLaterDataForHistoricalTarget() throws java.io.IOException {
        fixtureMappings();
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match()));
        FlaTeamMatchesSnapshotService snapshots = org.mockito.Mockito.mock(FlaTeamMatchesSnapshotService.class);
        for (long id : new long[]{270, 63}) {
            var page = new com.yukai.team.matchservice.opponentanalysis.parser.FlaTeamMatchesHtmlParser().parsePage(
                    new org.springframework.core.io.ClassPathResource("fla/team-" + id + ".html")
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8), id);
            when(snapshots.getTeamMatches(id, false)).thenReturn(new FlaTeamMatchesSnapshotService.Result(
                    page, "version-" + id, RecentFormCalculatorTest.FETCHED, CacheStatus.HIT, 21600, false));
        }
        var realRecentService = new FlaRecentFormService(snapshots, new RecentFormCalculator(), flaProperties());
        var result = new MatchOpponentMetricsServiceImpl(matchInfoRepository, flaTeamMappingRepository,
                flaStandingsSnapshotService, new OpponentMetricsCalculator(), flaProperties(), realRecentService).getMetrics(42L);
        assertThat(result.ourTeam().recentForm().status()).isEqualTo(
                com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics.Status.NO_COMPLETED_MATCHES);
        assertThat(result.ourTeam().recentFormSampleSize()).isZero();
        assertThat(result.ourTeam().recentFive()).isEmpty();
        assertThat(result.ourTeam().recentFiveWins()).isNull();
        assertThat(result.opponent().currentWinningStreak()).isNull();
        assertThat(result.comparison().recentFiveWinsDifference()).isNull();
        assertThat(result.ourTeam().points()).isEqualTo(24);
        assertThat(result.dataSource().snapshotId()).isEqualTo(10L);
        assertThat(result.dataSource().standingsTemporalScope()).isEqualTo("FETCH_TIME_SNAPSHOT_NOT_HISTORICAL_PREMATCH");
        var futureTarget = match();
        futureTarget.setMatchTime(RecentFormCalculatorTest.TARGET);
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(futureTarget));
        var future = new MatchOpponentMetricsServiceImpl(matchInfoRepository, flaTeamMappingRepository,
                flaStandingsSnapshotService, new OpponentMetricsCalculator(), flaProperties(), realRecentService).getMetrics(42L);
        assertThat(future.ourTeam().recentFive()).containsExactly("lost", "won", "won", "won", "won");
        assertThat(future.opponent().recentFive()).containsExactly("won", "won", "draw", "lost", "lost");
        assertThat(future.ourTeam().recentFormAvailable()).isTrue();
        assertThat(future.opponent().recentFormAvailable()).isTrue();
        assertThat(future.comparison().recentFiveWinsDifference()).isEqualTo(2);
        assertThat(future.dataSource()).isEqualTo(result.dataSource());
        assertThat(future.ourTeam().points()).isEqualTo(result.ourTeam().points());
    }

    @Test
    void oneUnavailableSideKeepsStandingsAndMakesRecentComparisonUnavailable() throws java.io.IOException {
        fixtureMappings();
        var available = new RecentFormCalculator().calculate(270L, RecentFormCalculatorTest.fixture(270),
                RecentFormCalculatorTest.TARGET,
                new com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics.Source(270L, null,
                        "a", "a", RecentFormCalculatorTest.FETCHED, CacheStatus.HIT, 21600, false, null));
        when(recentFormService.getForm(org.mockito.ArgumentMatchers.argThat(m -> m != null && m.getFlaTeamId().equals(270L)),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(false))).thenReturn(available);
        var result = service().getMetrics(42L);
        assertThat(result.ourTeam().recentFormAvailable()).isTrue();
        assertThat(result.opponent().recentFormAvailable()).isFalse();
        assertThat(result.opponent().recentFiveWins()).isNull();
        assertThat(result.comparison().recentFiveWinsDifference()).isNull();
        assertThat(result.comparison().pointsDifference()).isNotNull();
    }

    @Test
    void sameTeamIsResolvedOnlyOncePerMetricsRequest() {
        commonMappings();
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 13L))
                .thenReturn(Optional.of(mapping(13L, 6580L, 1365L, 15L)));
        when(flaStandingsSnapshotService.getStandings(1365L, 15L, false))
                .thenReturn(snapshotResult(entry(6580L, "Both", 20, 6, 2, 2, 30, 10)));
        service().getMetrics(42L);
        verify(recentFormService, org.mockito.Mockito.times(1)).getForm(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(false));
    }

    private void fixtureMappings() {
        var target = match();
        target.setMatchTime(RecentFormCalculatorTest.TARGET);
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(target));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.of(mapping(12L, 270L, 16L, 2L)));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 13L))
                .thenReturn(Optional.of(mapping(13L, 63L, 16L, 2L)));
        when(flaStandingsSnapshotService.getStandings(16L, 2L, false))
                .thenReturn(snapshotResult(entry(270L, "YEXIAO PARIS FC", 24, 5, 0, 1, 31, 9),
                        entry(63L, "18 EMPIRE", 16, 2, 1, 1, 11, 11)));
    }

    private void commonMappings() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match()));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.of(mapping(12L, 6580L, 1365L, 15L)));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 13L))
                .thenReturn(Optional.of(mapping(13L, 6581L, 1365L, 15L)));
    }

    private MatchOpponentMetricsServiceImpl service() {
        return new MatchOpponentMetricsServiceImpl(
                matchInfoRepository,
                flaTeamMappingRepository,
                flaStandingsSnapshotService,
                new OpponentMetricsCalculator(),
                flaProperties(),
                recentFormService
        );
    }

    private StandingsSnapshotResult snapshotResult(FlaStandingEntryResponse... standings) {
        return new StandingsSnapshotResult(
                10L,
                List.of(standings),
                payloadHash(),
                OffsetDateTime.parse("2026-08-05T12:00:00Z"),
                CacheStatus.HIT
        );
    }

    private String payloadHash() {
        return "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    }

    private FlaProperties flaProperties() {
        FlaProperties properties = new FlaProperties();
        properties.setSnapshotTtl(Duration.ofHours(6));
        return properties;
    }

    private MatchInfo match() {
        MatchInfo match = new MatchInfo();
        match.setId(42L);
        match.setTournamentId(5L);
        match.setTournamentNameSnapshot("Paris League");
        match.setSeasonSnapshot("2026");
        match.setOurTeamId(12L);
        match.setOurTeamNameSnapshot("Our Team");
        match.setOpponentTeamId(13L);
        match.setOpponentTeamNameSnapshot("Opponent Team");
        match.setMatchTime(OffsetDateTime.parse("2026-08-10T18:30:00Z"));
        match.setHomeAway(HomeAway.HOME);
        match.setMatchStatus(MatchStatus.SCHEDULED);
        return match;
    }

    private FlaTeamMapping mapping(Long internalTeamId, Long flaTeamId, Long championnatId, Long saisonId) {
        FlaTeamMapping mapping = new FlaTeamMapping();
        mapping.setInternalTournamentId(5L);
        mapping.setInternalTeamId(internalTeamId);
        mapping.setFlaChampionnatId(championnatId);
        mapping.setFlaSaisonId(saisonId);
        mapping.setFlaTeamId(flaTeamId);
        mapping.setFlaTeamName("FLA Team " + flaTeamId);
        return mapping;
    }

    private FlaStandingEntryResponse entry(
            Long teamId,
            String teamName,
            Integer points,
            Integer wins,
            Integer draws,
            Integer losses,
            Integer goalsFor,
            Integer goalsAgainst
    ) {
        FlaStandingEntryResponse entry = new FlaStandingEntryResponse();
        entry.setTeamId(teamId);
        entry.setTeamName(teamName);
        entry.setChampionnatId(1365L);
        entry.setPoints(points);
        entry.setWins(wins);
        entry.setDraws(draws);
        entry.setLosses(losses);
        entry.setGoalsFor(goalsFor);
        entry.setGoalsAgainst(goalsAgainst);
        entry.setBonus(0);
        entry.setForfeits(0);
        entry.setForm(List.of("won", "lost"));
        return entry;
    }
}
