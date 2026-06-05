package com.yukai.team.statisticsservice.service.query;

import com.yukai.team.statisticsservice.dto.response.MatchSummaryResponse;
import com.yukai.team.statisticsservice.dto.response.PagedResponse;
import com.yukai.team.statisticsservice.mapper.StatisticsResponseMapper;
import com.yukai.team.statisticsservice.repository.MatchSummaryProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchStatisticsQueryService {

    private final MatchSummaryProjectionRepository matchSummaryProjectionRepository;
    private final StatisticsResponseMapper mapper;

    @Transactional(readOnly = true)
    public PagedResponse<MatchSummaryResponse> findMatches(int page, int size, Long tournamentId) {
        var pageable = PageRequest.of(page, size);
        var result = tournamentId == null
                ? matchSummaryProjectionRepository.findAllByOrderByMatchTimeDesc(pageable)
                : matchSummaryProjectionRepository.findByTournamentIdOrderByMatchTimeDesc(tournamentId, pageable);

        return PagedResponse.<MatchSummaryResponse>builder()
                .content(result.getContent().stream().map(mapper::toMatchSummaryResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }
}
