package com.yukai.team.statisticsservice.dto.event.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchResultUpdatedEventData {

    private Long matchId;

    private Long tournamentId;

    private String tournamentNameSnapshot;

    private String seasonSnapshot;

    private Long ourTeamId;

    private String ourTeamNameSnapshot;

    private Long opponentTeamId;

    private String opponentTeamNameSnapshot;

    private OffsetDateTime matchTime;

    private String homeAway;

    private Integer oldOurScore;

    private Integer oldOpponentScore;

    private Integer newOurScore;

    private Integer newOpponentScore;

    private String matchStatus;

    private Boolean finished;
}
