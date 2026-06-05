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
public class MatchGoalUpdatedEventData {

    private Long goalId;
    private Long matchId;
    private Long tournamentId;
    private String tournamentNameSnapshot;
    private String seasonSnapshot;
    private OldGoal oldGoal;
    private NewGoal newGoal;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OldGoal {

        private Long playerId;
        private String playerNameSnapshot;
        private Integer jerseyNumberSnapshot;
        private String goalType;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewGoal {

        private Long playerId;
        private String playerNameSnapshot;
        private Integer jerseyNumberSnapshot;
        private String goalType;
    }
}
