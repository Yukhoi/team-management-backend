package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.dto.CreateMatchRequest;
import com.yukai.team.matchservice.dto.MatchResponse;
import com.yukai.team.matchservice.dto.PageResponse;
import com.yukai.team.matchservice.dto.ReplaceAppearanceRequest;
import com.yukai.team.matchservice.dto.UpdateMatchResultRequest;
import com.yukai.team.matchservice.entity.MatchStatus;

public interface MatchService {

    MatchResponse createMatch(CreateMatchRequest request);

    MatchResponse getMatch(Long id);

    PageResponse<MatchResponse> getMatches(int page, int size, Long tournamentId, MatchStatus status, String keyword);

    MatchResponse updateResult(Long id, UpdateMatchResultRequest request);

    MatchResponse replaceAppearances(Long id, ReplaceAppearanceRequest request);
}
