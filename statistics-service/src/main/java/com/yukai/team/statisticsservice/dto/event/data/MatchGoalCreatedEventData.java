package com.yukai.team.statisticsservice.dto.event.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchGoalCreatedEventData {

    private Long goalId;
    private Long matchId;
    private Long tournamentId;
    private String tournamentNameSnapshot;
    private String seasonSnapshot;
    private Long playerId;
    private String playerNameSnapshot;
    private Integer jerseyNumberSnapshot;
    private Integer goalMinute;
    private String goalType;
}
