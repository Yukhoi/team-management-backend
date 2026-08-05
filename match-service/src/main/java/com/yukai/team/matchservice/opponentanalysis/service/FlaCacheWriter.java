package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaSyncResponse;

import java.util.List;

public interface FlaCacheWriter {

    FlaSyncResponse upsertStandings(
            Long championnatId,
            Long saisonId,
            String championnatName,
            List<FlaStandingEntryResponse> standings
    );
}
