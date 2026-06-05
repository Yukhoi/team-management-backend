package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.client.TeamServiceClient;
import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.repository.MatchAppearanceRepository;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import com.yukai.team.matchservice.repository.MatchResultHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchInfoRepository matchInfoRepository;
    @Mock
    private MatchAppearanceRepository matchAppearanceRepository;
    @Mock
    private MatchResultHistoryRepository matchResultHistoryRepository;
    @Mock
    private OutboxEventService outboxEventService;
    @Mock
    private TeamServiceClient teamServiceClient;
    @Mock
    private TournamentClient tournamentClient;

    @Test
    void getMatchesReturnsPage() {
        MatchInfo match = new MatchInfo();
        match.setId(1L);
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "matchTime"));
        when(matchInfoRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(match), pageable, 1));
        when(matchAppearanceRepository.findByMatchIdOrderByStarterDescPlayerIdAsc(1L)).thenReturn(List.of());

        var response = service().getMatches(0, 20, null, null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(matchInfoRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getMatchesAcceptsTournamentAndStatusFilters() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "matchTime"));
        when(matchInfoRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var response = service().getMatches(0, 20, 7L, MatchStatus.FINISHED, null);

        assertThat(response.getContent()).isEmpty();
        verify(matchInfoRepository).findAll(any(Specification.class), eq(pageable));
    }

    private MatchServiceImpl service() {
        return new MatchServiceImpl(
                matchInfoRepository,
                matchAppearanceRepository,
                matchResultHistoryRepository,
                outboxEventService,
                teamServiceClient,
                tournamentClient
        );
    }
}
