package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaChampionnat;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeam;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaChampionnatRepository;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlaSyncServiceImplTest {

    @Mock
    private FlaClient flaClient;

    @Mock
    private FlaChampionnatRepository flaChampionnatRepository;

    @Mock
    private FlaTeamRepository flaTeamRepository;

    @Test
    void firstSyncCreatesChampionnatAndTeams() {
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, "A Team"), entry(6581L, "B Team")));
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.empty());
        when(flaTeamRepository.findByChampionnatIdAndSaisonIdAndFlaTeamIdIn(1365L, 15L, List.of(6580L, 6581L)))
                .thenReturn(List.of());

        var response = service().syncStandings(1365L, 15L, "Division 3");

        assertThat(response.getCreatedTeamCount()).isEqualTo(2);
        assertThat(response.getUpdatedTeamCount()).isZero();
        assertThat(response.getUnchangedTeamCount()).isZero();
        verify(flaChampionnatRepository).save(any(FlaChampionnat.class));
        verify(flaTeamRepository).saveAll(anyList());
    }

    @Test
    void existingSyncWithSameNamesIsUnchanged() {
        FlaChampionnat championnat = championnat("Division 3");
        FlaTeam team = team(6580L, "A Team");
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, "A Team")));
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.of(championnat));
        when(flaTeamRepository.findByChampionnatIdAndSaisonIdAndFlaTeamIdIn(1365L, 15L, List.of(6580L)))
                .thenReturn(List.of(team));

        var response = service().syncStandings(1365L, 15L, "Division 3");

        assertThat(response.getCreatedTeamCount()).isZero();
        assertThat(response.getUpdatedTeamCount()).isZero();
        assertThat(response.getUnchangedTeamCount()).isEqualTo(1);
        assertThat(championnat.getName()).isEqualTo("Division 3");
        verify(flaTeamRepository, never()).saveAll(anyList());
    }

    @Test
    void changedTeamNameIsUpdated() {
        FlaTeam team = team(6580L, "Old Team");
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, "New Team")));
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.of(championnat("Division 3")));
        when(flaTeamRepository.findByChampionnatIdAndSaisonIdAndFlaTeamIdIn(1365L, 15L, List.of(6580L)))
                .thenReturn(List.of(team));

        var response = service().syncStandings(1365L, 15L, "Division 3");

        assertThat(response.getUpdatedTeamCount()).isEqualTo(1);
        assertThat(team.getTeamName()).isEqualTo("New Team");
        verify(flaTeamRepository).saveAll(List.of(team));
    }

    @Test
    void partialExistingAndNewTeamsAreCounted() {
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, "A Team"), entry(6581L, "B Team")));
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.of(championnat("Division 3")));
        when(flaTeamRepository.findByChampionnatIdAndSaisonIdAndFlaTeamIdIn(1365L, 15L, List.of(6580L, 6581L)))
                .thenReturn(List.of(team(6580L, "A Team")));

        var response = service().syncStandings(1365L, 15L, "Division 3");

        assertThat(response.getCreatedTeamCount()).isEqualTo(1);
        assertThat(response.getUnchangedTeamCount()).isEqualTo(1);
    }

    @Test
    void mismatchedChampionnatFails() {
        FlaStandingEntryResponse entry = entry(6580L, "A Team");
        entry.setChampionnatId(999L);
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry));

        assertThatThrownBy(() -> service().syncStandings(1365L, 15L, "Division 3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FLA standing championnatId does not match request");
    }

    @Test
    void emptyStandingsStillSyncsChampionnat() {
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of());
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.empty());

        var response = service().syncStandings(1365L, 15L, null);

        assertThat(response.getChampionnatName()).isEqualTo("FLA championnat 1365");
        assertThat(response.getReceivedTeamCount()).isZero();
        verify(flaTeamRepository, never()).findByChampionnatIdAndSaisonIdAndFlaTeamIdIn(any(), any(), anyList());
        verify(flaTeamRepository, never()).saveAll(anyList());
    }

    @Test
    void oldTeamsAreNotDeleted() {
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, "A Team")));
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.of(championnat("Division 3")));
        when(flaTeamRepository.findByChampionnatIdAndSaisonIdAndFlaTeamIdIn(1365L, 15L, List.of(6580L)))
                .thenReturn(List.of());

        service().syncStandings(1365L, 15L, "Division 3");

        verify(flaTeamRepository, never()).delete(any(FlaTeam.class));
        verify(flaTeamRepository, never()).deleteAll();
    }

    @Test
    void duplicateFlaTeamIdDoesNotCreateDuplicateRows() {
        when(flaClient.getStandings(1365L, 15L)).thenReturn(List.of(entry(6580L, "Old"), entry(6580L, "Latest")));
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.of(championnat("Division 3")));
        when(flaTeamRepository.findByChampionnatIdAndSaisonIdAndFlaTeamIdIn(1365L, 15L, List.of(6580L)))
                .thenReturn(List.of());
        ArgumentCaptor<List<FlaTeam>> captor = ArgumentCaptor.forClass(List.class);

        var response = service().syncStandings(1365L, 15L, "Division 3");

        verify(flaTeamRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTeamName()).isEqualTo("Latest");
        assertThat(response.getReceivedTeamCount()).isEqualTo(2);
        assertThat(response.getCreatedTeamCount()).isEqualTo(1);
    }

    private FlaSyncServiceImpl service() {
        return new FlaSyncServiceImpl(
                flaClient,
                new FlaCacheWriterImpl(flaChampionnatRepository, flaTeamRepository)
        );
    }

    private FlaStandingEntryResponse entry(Long teamId, String teamName) {
        FlaStandingEntryResponse response = new FlaStandingEntryResponse();
        response.setChampionnatId(1365L);
        response.setTeamId(teamId);
        response.setTeamName(teamName);
        return response;
    }

    private FlaChampionnat championnat(String name) {
        FlaChampionnat championnat = new FlaChampionnat();
        championnat.setChampionnatId(1365L);
        championnat.setSaisonId(15L);
        championnat.setName(name);
        return championnat;
    }

    private FlaTeam team(Long flaTeamId, String name) {
        FlaTeam team = new FlaTeam();
        team.setChampionnatId(1365L);
        team.setSaisonId(15L);
        team.setFlaTeamId(flaTeamId);
        team.setTeamName(name);
        return team;
    }
}
