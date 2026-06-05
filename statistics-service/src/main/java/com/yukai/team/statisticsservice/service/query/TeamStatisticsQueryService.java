package com.yukai.team.statisticsservice.service.query;

import com.yukai.team.statisticsservice.dto.response.TeamStatsResponse;
import com.yukai.team.statisticsservice.mapper.StatisticsResponseMapper;
import com.yukai.team.statisticsservice.repository.TeamStatsProjectionRepository;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheKeyBuilder;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamStatisticsQueryService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final TeamStatsProjectionRepository teamStatsProjectionRepository;
    private final StatisticsResponseMapper mapper;
    private final StatisticsCacheService statisticsCacheService;

    @Transactional(readOnly = true)
    public List<TeamStatsResponse> findTeams(String season, Long tournamentId) {
        String cacheKey = StatisticsCacheKeyBuilder.teams(season, tournamentId);
        Optional<TeamStatsResponse[]> cached = statisticsCacheService.get(cacheKey, TeamStatsResponse[].class);
        if (cached.isPresent()) {
            log.info("TeamStats Cache HIT, key={}", cacheKey);
            return List.of(cached.get());
        }
        log.info("TeamStats Cache MISS, key={}", cacheKey);

        List<TeamStatsResponse> response =
                teamStatsProjectionRepository.findBySeasonAndTournamentIdOrderByRankNoAsc(season, tournamentId)
                .stream()
                .map(mapper::toTeamStatsResponse)
                .toList();
        statisticsCacheService.put(cacheKey, response.toArray(TeamStatsResponse[]::new), CACHE_TTL);
        return response;
    }
}
