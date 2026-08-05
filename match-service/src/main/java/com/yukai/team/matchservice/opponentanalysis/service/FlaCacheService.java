package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaChampionnatResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamResponse;

import java.util.List;

public interface FlaCacheService {

    List<FlaChampionnatResponse> getChampionnats();

    List<FlaTeamResponse> getTeams(Long championnatId);

    List<FlaTeamResponse> getTeams(Long championnatId, Long saisonId);
}
