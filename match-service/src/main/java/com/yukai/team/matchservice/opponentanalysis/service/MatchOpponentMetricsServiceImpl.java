package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMapping;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAnalysisConflictException;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamMappingRepository;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MatchOpponentMetricsServiceImpl implements MatchOpponentMetricsService {

    private static final String PROVIDER_FLA = "FLA";
    private static final String FORM_ORDER_LATEST_TO_OLDEST = "LATEST_TO_OLDEST";
    private static final String RANKING_TYPE_CALCULATED = "CALCULATED";

    private final MatchInfoRepository matchInfoRepository;
    private final FlaTeamMappingRepository flaTeamMappingRepository;
    private final FlaClient flaClient;
    private final OpponentMetricsCalculator opponentMetricsCalculator;

    public MatchOpponentMetricsServiceImpl(
            MatchInfoRepository matchInfoRepository,
            FlaTeamMappingRepository flaTeamMappingRepository,
            FlaClient flaClient,
            OpponentMetricsCalculator opponentMetricsCalculator
    ) {
        this.matchInfoRepository = matchInfoRepository;
        this.flaTeamMappingRepository = flaTeamMappingRepository;
        this.flaClient = flaClient;
        this.opponentMetricsCalculator = opponentMetricsCalculator;
    }

    @Override
    public MatchOpponentMetricsResponse getMetrics(Long matchId) {
        validatePositive(matchId, "matchId");
        MatchInfo match = matchInfoRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found"));

        FlaTeamMapping ourMapping = findMapping(
                match.getTournamentId(),
                match.getOurTeamId(),
                "Our team FLA mapping is missing"
        );
        FlaTeamMapping opponentMapping = findMapping(
                match.getTournamentId(),
                match.getOpponentTeamId(),
                "Opponent team FLA mapping is missing"
        );
        validateSameExternalContext(ourMapping, opponentMapping);

        List<FlaStandingEntryResponse> standings = flaClient.getStandings(
                ourMapping.getFlaChampionnatId(),
                ourMapping.getFlaSaisonId()
        );
        FlaStandingEntryResponse ourEntry = findStandingEntry(
                standings,
                ourMapping.getFlaTeamId(),
                "FLA standings do not contain our mapped team"
        );
        FlaStandingEntryResponse opponentEntry = findStandingEntry(
                standings,
                opponentMapping.getFlaTeamId(),
                "FLA standings do not contain opponent mapped team"
        );
        Map<Long, Integer> ranks = opponentMetricsCalculator.calculateRanks(standings);
        TeamPerformanceMetrics ourMetrics = opponentMetricsCalculator.calculateTeamMetrics(
                match.getOurTeamId(),
                ourMapping.getFlaTeamId(),
                ourEntry,
                ranks.get(ourMapping.getFlaTeamId())
        );
        TeamPerformanceMetrics opponentMetrics = opponentMetricsCalculator.calculateTeamMetrics(
                match.getOpponentTeamId(),
                opponentMapping.getFlaTeamId(),
                opponentEntry,
                ranks.get(opponentMapping.getFlaTeamId())
        );

        return new MatchOpponentMetricsResponse(
                toMatchContext(match),
                ourMetrics,
                opponentMetrics,
                opponentMetricsCalculator.calculateLeagueMetrics(standings),
                opponentMetricsCalculator.compare(ourMetrics, opponentMetrics),
                new MatchOpponentMetricsResponse.DataSource(
                        PROVIDER_FLA,
                        ourMapping.getFlaChampionnatId(),
                        ourMapping.getFlaSaisonId(),
                        OffsetDateTime.now(),
                        FORM_ORDER_LATEST_TO_OLDEST,
                        RANKING_TYPE_CALCULATED
                )
        );
    }

    private FlaTeamMapping findMapping(Long tournamentId, Long teamId, String message) {
        return flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(tournamentId, teamId)
                .orElseThrow(() -> new OpponentAnalysisConflictException(message));
    }

    private void validateSameExternalContext(FlaTeamMapping ourMapping, FlaTeamMapping opponentMapping) {
        if (!Objects.equals(ourMapping.getFlaChampionnatId(), opponentMapping.getFlaChampionnatId())) {
            throw new OpponentAnalysisConflictException("Our team and opponent FLA championnat do not match");
        }
        if (!Objects.equals(ourMapping.getFlaSaisonId(), opponentMapping.getFlaSaisonId())) {
            throw new OpponentAnalysisConflictException("Our team and opponent FLA saison do not match");
        }
    }

    private FlaStandingEntryResponse findStandingEntry(
            List<FlaStandingEntryResponse> standings,
            Long flaTeamId,
            String message
    ) {
        if (standings == null || standings.isEmpty()) {
            throw new OpponentAnalysisConflictException(message);
        }
        return standings.stream()
                .filter(Objects::nonNull)
                .filter(entry -> Objects.equals(entry.getTeamId(), flaTeamId))
                .findFirst()
                .orElseThrow(() -> new OpponentAnalysisConflictException(message));
    }

    private MatchOpponentMetricsResponse.MatchContext toMatchContext(MatchInfo match) {
        return new MatchOpponentMetricsResponse.MatchContext(
                match.getId(),
                match.getTournamentId(),
                match.getTournamentNameSnapshot(),
                match.getSeasonSnapshot(),
                match.getOurTeamId(),
                match.getOurTeamNameSnapshot(),
                match.getOpponentTeamId(),
                match.getOpponentTeamNameSnapshot(),
                match.getMatchTime(),
                match.getHomeAway(),
                match.getMatchStatus()
        );
    }

    private void validatePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
