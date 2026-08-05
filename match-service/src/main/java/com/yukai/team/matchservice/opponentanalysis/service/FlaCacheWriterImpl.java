package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaSyncResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaChampionnat;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeam;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaChampionnatRepository;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FlaCacheWriterImpl implements FlaCacheWriter {

    private final FlaChampionnatRepository flaChampionnatRepository;
    private final FlaTeamRepository flaTeamRepository;

    public FlaCacheWriterImpl(
            FlaChampionnatRepository flaChampionnatRepository,
            FlaTeamRepository flaTeamRepository
    ) {
        this.flaChampionnatRepository = flaChampionnatRepository;
        this.flaTeamRepository = flaTeamRepository;
    }

    @Override
    @Transactional
    public FlaSyncResponse upsertStandings(
            Long championnatId,
            Long saisonId,
            String championnatName,
            List<FlaStandingEntryResponse> standings
    ) {
        String resolvedChampionnatName = resolveChampionnatName(championnatId, championnatName);
        FlaChampionnat championnat = flaChampionnatRepository
                .findByChampionnatIdAndSaisonId(championnatId, saisonId)
                .orElseGet(FlaChampionnat::new);
        championnat.setChampionnatId(championnatId);
        championnat.setSaisonId(saisonId);
        championnat.setName(resolvedChampionnatName);
        flaChampionnatRepository.save(championnat);

        Map<Long, FlaStandingEntryResponse> standingsByTeamId = deduplicateByTeamId(standings);
        Map<Long, FlaTeam> existingTeams = standingsByTeamId.isEmpty()
                ? Map.of()
                : flaTeamRepository
                        .findByChampionnatIdAndSaisonIdAndFlaTeamIdIn(
                                championnatId,
                                saisonId,
                                standingsByTeamId.keySet().stream().toList()
                        )
                        .stream()
                        .collect(Collectors.toMap(FlaTeam::getFlaTeamId, Function.identity()));

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        List<FlaTeam> teamsToSave = new java.util.ArrayList<>();

        for (FlaStandingEntryResponse standing : standingsByTeamId.values()) {
            Long teamId = standing.getTeamId();
            String teamName = resolveTeamName(teamId, standing.getTeamName());
            FlaTeam team = existingTeams.get(teamId);
            if (team == null) {
                team = new FlaTeam();
                team.setChampionnatId(championnatId);
                team.setSaisonId(saisonId);
                team.setFlaTeamId(teamId);
                team.setTeamName(teamName);
                teamsToSave.add(team);
                created++;
            } else if (!Objects.equals(team.getTeamName(), teamName)) {
                team.setTeamName(teamName);
                teamsToSave.add(team);
                updated++;
            } else {
                unchanged++;
            }
        }

        if (!teamsToSave.isEmpty()) {
            flaTeamRepository.saveAll(teamsToSave);
        }

        return new FlaSyncResponse(
                championnatId,
                saisonId,
                resolvedChampionnatName,
                standings.size(),
                created,
                updated,
                unchanged,
                OffsetDateTime.now()
        );
    }

    private Map<Long, FlaStandingEntryResponse> deduplicateByTeamId(List<FlaStandingEntryResponse> standings) {
        Map<Long, FlaStandingEntryResponse> standingsByTeamId = new LinkedHashMap<>();
        for (FlaStandingEntryResponse standing : standings) {
            standingsByTeamId.put(standing.getTeamId(), standing);
        }
        return standingsByTeamId;
    }

    private String resolveChampionnatName(Long championnatId, String championnatName) {
        if (championnatName != null && !championnatName.isBlank()) {
            return championnatName.trim();
        }
        return "FLA championnat " + championnatId;
    }

    private String resolveTeamName(Long teamId, String teamName) {
        if (teamName != null && !teamName.isBlank()) {
            return teamName.trim();
        }
        return "FLA team " + teamId;
    }
}
