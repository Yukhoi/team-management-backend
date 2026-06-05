package com.yukai.team.statisticsservice.service.projection;

import com.yukai.team.statisticsservice.dto.event.data.MatchResultUpdatedEventData;
import com.yukai.team.statisticsservice.entity.TeamStatsProjection;
import com.yukai.team.statisticsservice.repository.TeamStatsProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamStatsProjectionService {

    private final TeamStatsProjectionRepository teamStatsProjectionRepository;

    @Transactional
    public void applyMatchResultUpdated(MatchResultUpdatedEventData data) {
        TeamStatsProjection projection = getOrCreate(data);

        if (data.getOldOurScore() != null && data.getOldOpponentScore() != null) {
            projection.rollbackResult(data.getOldOurScore(), data.getOldOpponentScore());
        }

        if (Boolean.TRUE.equals(data.getFinished())) {
            projection.applyResult(safe(data.getNewOurScore()), safe(data.getNewOpponentScore()));
        }

        projection.recalculateGoalDiff();
        teamStatsProjectionRepository.save(projection);
    }

    private TeamStatsProjection getOrCreate(MatchResultUpdatedEventData data) {
        return teamStatsProjectionRepository.findByTeamIdAndSeasonAndTournamentId(
                data.getOurTeamId(),
                data.getSeasonSnapshot(),
                data.getTournamentId()
        ).map(existing -> {
            existing.setTeamNameSnapshot(data.getOurTeamNameSnapshot());
            existing.setTournamentNameSnapshot(data.getTournamentNameSnapshot());
            return existing;
        }).orElseGet(() -> teamStatsProjectionRepository.save(TeamStatsProjection.builder()
                .teamId(data.getOurTeamId())
                .teamNameSnapshot(data.getOurTeamNameSnapshot())
                .season(data.getSeasonSnapshot())
                .tournamentId(data.getTournamentId())
                .tournamentNameSnapshot(data.getTournamentNameSnapshot())
                .played(0)
                .win(0)
                .draw(0)
                .lose(0)
                .goalsFor(0)
                .goalsAgainst(0)
                .goalDiff(0)
                .points(0)
                .build()));
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
