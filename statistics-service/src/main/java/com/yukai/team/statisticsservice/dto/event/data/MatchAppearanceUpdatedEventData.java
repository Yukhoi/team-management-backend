package com.yukai.team.statisticsservice.dto.event.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchAppearanceUpdatedEventData {

    private Long matchId;
    private Long tournamentId;
    private String tournamentNameSnapshot;
    private String seasonSnapshot;
    private List<AppearanceItem> appearances;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppearanceItem {

        private Long playerId;
        private String playerNameSnapshot;
        private Integer jerseyNumberSnapshot;
        private String positionSnapshot;
        private Boolean appeared;
        private Boolean starter;
        private Integer onMinute;
        private Integer offMinute;
    }
}
