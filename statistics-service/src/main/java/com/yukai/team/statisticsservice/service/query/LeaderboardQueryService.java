package com.yukai.team.statisticsservice.service.query;

import com.yukai.team.statisticsservice.dto.response.LeaderboardResponse;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.mapper.StatisticsResponseMapper;
import com.yukai.team.statisticsservice.repository.LeaderboardProjectionRepository;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheKeyBuilder;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardQueryService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final LeaderboardProjectionRepository leaderboardProjectionRepository;
    private final StatisticsResponseMapper mapper;
    private final StatisticsCacheService statisticsCacheService;

    @Transactional(readOnly = true)
    public List<LeaderboardResponse> findLeaderboard(String boardType, String season, Long tournamentId, int topN) {
        LeaderboardBoardType parsedBoardType = parseBoardType(boardType);
        String cacheKey = StatisticsCacheKeyBuilder.leaderboard(parsedBoardType, season, tournamentId, topN);
        Optional<LeaderboardResponse[]> cached = statisticsCacheService.get(cacheKey, LeaderboardResponse[].class);
        if (cached.isPresent()) {
            log.info("Leaderboard Cache HIT, key={}", cacheKey);
            return List.of(cached.get());
        }
        log.info("Leaderboard Cache MISS, key={}", cacheKey);

        List<LeaderboardResponse> response = leaderboardProjectionRepository
                .findByBoardTypeAndSeasonAndTournamentIdOrderByRankNoAsc(parsedBoardType, season, tournamentId)
                .stream()
                .limit(topN)
                .map(mapper::toLeaderboardResponse)
                .toList();
        statisticsCacheService.put(cacheKey, response.toArray(LeaderboardResponse[]::new), CACHE_TTL);
        return response;
    }

    private LeaderboardBoardType parseBoardType(String boardType) {
        return Arrays.stream(LeaderboardBoardType.values())
                .filter(type -> type.name().equals(boardType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported boardType: " + boardType));
    }
}
