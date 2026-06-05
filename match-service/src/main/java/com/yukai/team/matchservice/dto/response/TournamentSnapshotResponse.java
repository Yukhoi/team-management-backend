package com.yukai.team.matchservice.dto.response;

import lombok.Data;

@Data
public class TournamentSnapshotResponse {

    private Long id;
    private String name;
    private String season;
    private String tournamentType;
    private String status;
}
