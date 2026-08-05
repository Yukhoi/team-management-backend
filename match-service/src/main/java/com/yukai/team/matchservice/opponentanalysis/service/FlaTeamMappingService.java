package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMappingResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.UpsertFlaTeamMappingRequest;

import java.util.List;

public interface FlaTeamMappingService {

    FlaTeamMappingResponse upsertMapping(Long tournamentId, Long teamId, UpsertFlaTeamMappingRequest request);

    List<FlaTeamMappingResponse> getMappings(Long tournamentId);

    FlaTeamMappingResponse getMapping(Long tournamentId, Long teamId);

    void deleteMapping(Long tournamentId, Long teamId);
}
