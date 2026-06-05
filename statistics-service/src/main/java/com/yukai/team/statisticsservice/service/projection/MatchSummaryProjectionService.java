package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.dto.event.data.MatchCreatedEventData;
import com.yukai.team.statisticsservice.dto.event.data.MatchResultUpdatedEventData;
import com.yukai.team.statisticsservice.entity.MatchSummaryProjection;
import com.yukai.team.statisticsservice.entity.enums.HomeAwayType;
import com.yukai.team.statisticsservice.entity.enums.MatchStatus;
import com.yukai.team.statisticsservice.repository.MatchSummaryProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchSummaryProjectionService {

    private final MatchSummaryProjectionRepository matchSummaryProjectionRepository;

    @Transactional
    public void upsertMatchCreated(MatchCreatedEventData eventData) {
        MatchSummaryProjection projection = matchSummaryProjectionRepository.findByMatchId(eventData.getMatchId())
                .orElse(null);
        boolean newProjection = projection == null;
        if (newProjection) {
            projection = new MatchSummaryProjection();
        }
        boolean preserveResultFields = !newProjection && Boolean.TRUE.equals(projection.getFinished());

        projection.setMatchId(eventData.getMatchId());
        applyCommonSnapshot(
                projection,
                eventData.getTournamentId(),
                eventData.getTournamentNameSnapshot(),
                eventData.getSeasonSnapshot(),
                eventData.getMatchTime(),
                eventData.getOpponentTeamNameSnapshot(),
                eventData.getHomeAway(),
                eventData.getMatchStatus(),
                eventData.getFinished()
        );
        if (preserveResultFields) {
            projection.setMatchStatus(MatchStatus.FINISHED);
            projection.setFinished(true);
        } else {
            projection.setOurScore(eventData.getOurScore());
            projection.setOpponentScore(eventData.getOpponentScore());
            projection.setMatchStatus(MatchStatus.valueOf(eventData.getMatchStatus()));
            projection.setFinished(eventData.getFinished());
        }

        matchSummaryProjectionRepository.save(projection);
    }

    @Transactional
    public void upsertMatchResultUpdated(MatchResultUpdatedEventData eventData) {
        MatchSummaryProjection projection = matchSummaryProjectionRepository.findByMatchId(eventData.getMatchId())
                .orElseGet(MatchSummaryProjection::new);

        projection.setMatchId(eventData.getMatchId());
        applyCommonSnapshot(
                projection,
                eventData.getTournamentId(),
                eventData.getTournamentNameSnapshot(),
                eventData.getSeasonSnapshot(),
                eventData.getMatchTime(),
                eventData.getOpponentTeamNameSnapshot(),
                eventData.getHomeAway(),
                eventData.getMatchStatus(),
                eventData.getFinished()
        );
        projection.setOurScore(eventData.getNewOurScore());
        projection.setOpponentScore(eventData.getNewOpponentScore());

        matchSummaryProjectionRepository.save(projection);
    }

    private void applyCommonSnapshot(
            MatchSummaryProjection projection,
            Long tournamentId,
            String tournamentNameSnapshot,
            String seasonSnapshot,
            java.time.OffsetDateTime matchTime,
            String opponentTeamNameSnapshot,
            String homeAway,
            String matchStatus,
            Boolean finished
    ) {
        projection.setTournamentId(tournamentId);
        projection.setTournamentNameSnapshot(tournamentNameSnapshot);
        projection.setSeasonSnapshot(seasonSnapshot);
        projection.setMatchTime(matchTime);
        projection.setOpponentTeamNameSnapshot(opponentTeamNameSnapshot);
        projection.setHomeAway(HomeAwayType.valueOf(homeAway));
        projection.setMatchStatus(MatchStatus.valueOf(matchStatus));
        projection.setFinished(finished);
    }
}
