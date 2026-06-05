package com.yukai.team.statisticsservice.service.query;

import com.yukai.team.statisticsservice.dto.request.PlayerStatsSortField;
import com.yukai.team.statisticsservice.dto.response.PagedResponse;
import com.yukai.team.statisticsservice.dto.response.PlayerStatsResponse;
import com.yukai.team.statisticsservice.mapper.StatisticsResponseMapper;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerStatisticsQueryService {

    private final PlayerStatsProjectionRepository playerStatsProjectionRepository;
    private final StatisticsResponseMapper mapper;

    @Transactional(readOnly = true)
    public PagedResponse<PlayerStatsResponse> findPlayers(
            String season,
            Long tournamentId,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        String sortProperty = PlayerStatsSortField.from(sortBy).getProperty();
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        var result = playerStatsProjectionRepository.findBySeasonAndTournamentId(
                season,
                tournamentId,
                PageRequest.of(page, size, Sort.by(sortDirection, sortProperty).and(Sort.by("playerId")))
        );

        return PagedResponse.<PlayerStatsResponse>builder()
                .content(result.getContent().stream().map(mapper::toPlayerStatsResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }
}
