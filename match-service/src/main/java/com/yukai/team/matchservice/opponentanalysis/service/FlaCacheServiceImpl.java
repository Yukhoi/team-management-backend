package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaChampionnatResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaChampionnat;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeam;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaChampionnatRepository;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FlaCacheServiceImpl implements FlaCacheService {

    private final FlaChampionnatRepository flaChampionnatRepository;
    private final FlaTeamRepository flaTeamRepository;

    public FlaCacheServiceImpl(
            FlaChampionnatRepository flaChampionnatRepository,
            FlaTeamRepository flaTeamRepository
    ) {
        this.flaChampionnatRepository = flaChampionnatRepository;
        this.flaTeamRepository = flaTeamRepository;
    }

    @Override
    public List<FlaChampionnatResponse> getChampionnats() {
        return flaChampionnatRepository.findAllByOrderByNameAscSaisonIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<FlaTeamResponse> getTeams(Long championnatId) {
        return flaTeamRepository.findByChampionnatIdOrderByTeamNameAsc(championnatId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<FlaTeamResponse> getTeams(Long championnatId, Long saisonId) {
        return flaTeamRepository.findByChampionnatIdAndSaisonIdOrderByTeamNameAsc(championnatId, saisonId).stream()
                .map(this::toResponse)
                .toList();
    }

    private FlaChampionnatResponse toResponse(FlaChampionnat championnat) {
        return new FlaChampionnatResponse(
                championnat.getId(),
                championnat.getChampionnatId(),
                championnat.getSaisonId(),
                championnat.getName()
        );
    }

    private FlaTeamResponse toResponse(FlaTeam team) {
        return new FlaTeamResponse(
                team.getId(),
                team.getChampionnatId(),
                team.getSaisonId(),
                team.getFlaTeamId(),
                team.getTeamName()
        );
    }
}
