package com.yukai.team.tournamentservice.service;

import com.yukai.team.tournamentservice.dto.request.CreateTournamentRequest;
import com.yukai.team.tournamentservice.dto.request.UpdateTournamentRequest;
import com.yukai.team.tournamentservice.dto.response.PageResponse;
import com.yukai.team.tournamentservice.dto.response.TournamentResponse;
import com.yukai.team.tournamentservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.tournamentservice.entity.enums.TournamentStatus;
import com.yukai.team.tournamentservice.entity.enums.TournamentType;
import org.springframework.data.domain.Pageable;

public interface TournamentService {

    TournamentResponse createTournament(CreateTournamentRequest request);

    TournamentResponse updateTournament(Long id, UpdateTournamentRequest request);

    TournamentResponse getTournament(Long id);

    PageResponse<TournamentResponse> getTournaments(
            String season,
            TournamentStatus status,
            TournamentType tournamentType,
            Pageable pageable
    );

    TournamentResponse finishTournament(Long id);

    TournamentResponse cancelTournament(Long id);

    TournamentSnapshotResponse getTournamentSnapshot(Long id);
}
