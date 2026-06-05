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
public class MatchAssistUpsertedEventData {

    private Long assistId;
    private Long goalId;
    private Long matchId;
    private Long tournamentId;
    private String tournamentNameSnapshot;
    private String seasonSnapshot;
    private OldAssist oldAssist;
    private NewAssist newAssist;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OldAssist {

        private Long playerId;
        private String playerNameSnapshot;
        private Integer jerseyNumberSnapshot;
        private Integer assistMinute;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewAssist {

        private Long playerId;
        private String playerNameSnapshot;
        private Integer jerseyNumberSnapshot;
        private Integer assistMinute;
    }
}
