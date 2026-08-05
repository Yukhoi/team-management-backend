package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchOpponentMetricsServiceImplTest {

    @Mock
    private MatchInfoRepository matchInfoRepository;
    @Mock
    private FlaTeamMappingRepository flaTeamMappingRepository;
    @Mock
    private FlaClient flaClient;

    @Test
    void returnsMetrics() {
        MatchInfo match = match();
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.of(mapping(12L, 6580L, 1365L, 15L)));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 13L))
                .thenReturn(Optional.of(mapping(13L, 6581L, 1365L, 15L)));
        when(flaClient.getStandings(1365L, 15L))
                .thenReturn(List.of(
                        entry(6580L, "Our FLA", 20, 6, 2, 2, 30, 10),
                        entry(6581L, "Opponent FLA", 10, 3, 1, 6, 10, 20)
                ));

        var response = service().getMetrics(42L);

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
        assertThat(response.dataSource().formOrder()).isEqualTo("LATEST_TO_OLDEST");
        assertThat(response.dataSource().rankingType()).isEqualTo("CALCULATED");
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
        when(flaClient.getStandings(1365L, 15L))
                .thenReturn(List.of(entry(6581L, "Opponent FLA", 10, 3, 1, 6, 10, 20)));

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("FLA standings do not contain our mapped team");
    }

    @Test
    void failsWhenFlaStandingsMissOpponentTeam() {
        commonMappings();
        when(flaClient.getStandings(1365L, 15L))
                .thenReturn(List.of(entry(6580L, "Our FLA", 20, 6, 2, 2, 30, 10)));

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("FLA standings do not contain opponent mapped team");
    }

    @Test
    void failsWhenFlaStandingsAreEmpty() {
        commonMappings();
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of());

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .hasMessage("FLA standings do not contain our mapped team");
    }

    @Test
    void propagatesFlaClientException() {
        commonMappings();
        when(flaClient.getStandings(1365L, 15L))
                .thenThrow(new FlaClientException(HttpStatus.BAD_GATEWAY, "FLA_SERVICE_UNAVAILABLE", "FLA service unavailable"));

        assertThatThrownBy(() -> service().getMetrics(42L))
                .isInstanceOf(FlaClientException.class)
                .hasMessage("FLA service unavailable");
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
                flaClient,
                new OpponentMetricsCalculator()
        );
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
