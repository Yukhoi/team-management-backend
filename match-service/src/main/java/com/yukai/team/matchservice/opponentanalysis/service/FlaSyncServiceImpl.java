package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaSyncResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlaSyncServiceImpl implements FlaSyncService {

    private final FlaClient flaClient;
    private final FlaCacheWriter flaCacheWriter;

    public FlaSyncServiceImpl(FlaClient flaClient, FlaCacheWriter flaCacheWriter) {
        this.flaClient = flaClient;
        this.flaCacheWriter = flaCacheWriter;
    }

    @Override
    public FlaSyncResponse syncStandings(Long championnatId, Long saisonId, String championnatName) {
        validateId(championnatId, "championnatId");
        validateId(saisonId, "saisonId");

        List<FlaStandingEntryResponse> standings = flaClient.getStandings(championnatId, saisonId);
        validateStandings(championnatId, standings);

        return flaCacheWriter.upsertStandings(championnatId, saisonId, championnatName, standings);
    }

    private void validateStandings(Long championnatId, List<FlaStandingEntryResponse> standings) {
        for (FlaStandingEntryResponse standing : standings) {
            if (!championnatId.equals(standing.getChampionnatId())) {
                throw new IllegalArgumentException("FLA standing championnatId does not match request");
            }
            if (standing.getTeamId() == null) {
                throw new IllegalArgumentException("FLA standing teamId is required");
            }
        }
    }

    private void validateId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
