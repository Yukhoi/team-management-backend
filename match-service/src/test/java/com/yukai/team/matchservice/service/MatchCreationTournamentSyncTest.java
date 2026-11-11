package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.client.*;
import com.yukai.team.matchservice.client.dto.*;
import com.yukai.team.matchservice.dto.CreateMatchRequest;
import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.repository.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class MatchCreationTournamentSyncTest {
    final MatchInfoRepository matches = mock(MatchInfoRepository.class);
    final OutboxEventService outbox = mock(OutboxEventService.class);
    final TeamServiceClient teams = mock(TeamServiceClient.class);
    final TournamentClient tournaments = mock(TournamentClient.class);
    final TournamentSyncService sync = mock(TournamentSyncService.class);
    final MatchServiceImpl service = new MatchServiceImpl(matches,mock(MatchAppearanceRepository.class),mock(MatchResultHistoryRepository.class),outbox,teams,tournaments,sync);

    @Test
    void creationKeepsOriginalFieldsAndUsesMetadataResolvedUnderSyncLock() {
        var request = new CreateMatchRequest(); request.setTournamentId(5L); request.setOurTeamId(1L); request.setOpponentTeamId(2L);
        request.setMatchTime(OffsetDateTime.parse("2027-01-01T18:00:00Z")); request.setHomeAway(HomeAway.HOME); request.setVenue("Venue");
        var our = new InternalTeamInfo(); our.setId(1L); our.setName("Our");
        var opponent = new InternalTeamInfo(); opponent.setId(2L); opponent.setName("Opponent");
        var valid = new ValidateMatchTeamsResponse(); valid.setValid(true); valid.setOurTeam(our); valid.setOpponentTeam(opponent);
        when(teams.validateMatchTeams(1L,2L)).thenReturn(valid);
        var oldSnapshot = TournamentSyncIntegrationTest.snapshot("Old HTTP","2025",1L);
        when(tournaments.getTournamentSnapshot(5L)).thenReturn(oldSnapshot);
        when(sync.prepareForCreation(oldSnapshot)).thenReturn(TournamentSyncIntegrationTest.snapshot("Latest","2027",3L));
        when(matches.save(any())).thenAnswer(call -> { MatchInfo row=call.getArgument(0); row.setId(11L); return row; });
        var response = service.createMatch(request);
        assertThat(response.getTournamentNameSnapshot()).isEqualTo("Latest");
        assertThat(response.getSeasonSnapshot()).isEqualTo("2027");
        assertThat(response.getOurTeamId()).isEqualTo(1L);
        assertThat(response.getOpponentTeamId()).isEqualTo(2L);
        assertThat(response.getOurScore()).isZero();
        var order = inOrder(sync,matches,outbox);
        order.verify(sync).prepareForCreation(oldSnapshot); order.verify(matches).save(any());
        order.verify(outbox).saveEvent(eq("match"),eq(11L),eq("match.created"),any());
        oldSnapshot.setStatus("FINISHED");
        assertThatThrownBy(() -> service.createMatch(request)).isInstanceOf(com.yukai.team.matchservice.exception.BusinessException.class);
    }
}
