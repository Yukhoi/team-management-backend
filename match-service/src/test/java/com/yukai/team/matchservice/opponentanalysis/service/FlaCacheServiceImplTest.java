package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaChampionnat;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeam;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaChampionnatRepository;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlaCacheServiceImplTest {

    @Mock
    private FlaChampionnatRepository flaChampionnatRepository;

    @Mock
    private FlaTeamRepository flaTeamRepository;

    @Test
    void getChampionnatsReturnsDisplayFields() {
        FlaChampionnat championnat = new FlaChampionnat();
        championnat.setId(1L);
        championnat.setChampionnatId(101L);
        championnat.setSaisonId(2026L);
        championnat.setName("Division 1");

        when(flaChampionnatRepository.findAllByOrderByNameAscSaisonIdDesc())
                .thenReturn(List.of(championnat));

        var response = service().getChampionnats();

        assertThat(response).hasSize(1);

        var first = response.get(0);
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getChampionnatId()).isEqualTo(101L);
        assertThat(first.getSaisonId()).isEqualTo(2026L);
        assertThat(first.getName()).isEqualTo("Division 1");

        verify(flaChampionnatRepository)
                .findAllByOrderByNameAscSaisonIdDesc();
    }

    @Test
    void getChampionnatsReturnsEmptyListWhenRepositoryReturnsEmptyList() {
        when(flaChampionnatRepository.findAllByOrderByNameAscSaisonIdDesc())
                .thenReturn(List.of());

        var response = service().getChampionnats();

        assertThat(response).isNotNull();
        assertThat(response).isEmpty();

        verify(flaChampionnatRepository)
                .findAllByOrderByNameAscSaisonIdDesc();
    }

    @Test
    void getChampionnatsKeepsRepositoryOrder() {
        FlaChampionnat first = new FlaChampionnat();
        first.setId(1L);
        first.setChampionnatId(101L);
        first.setSaisonId(2026L);
        first.setName("Division 1");

        FlaChampionnat second = new FlaChampionnat();
        second.setId(2L);
        second.setChampionnatId(101L);
        second.setSaisonId(2025L);
        second.setName("Division 1");

        FlaChampionnat third = new FlaChampionnat();
        third.setId(3L);
        third.setChampionnatId(102L);
        third.setSaisonId(2026L);
        third.setName("Division 2");

        when(flaChampionnatRepository.findAllByOrderByNameAscSaisonIdDesc())
                .thenReturn(List.of(first, second, third));

        var response = service().getChampionnats();

        assertThat(response)
                .extracting(
                        item -> item.getName() + "-" + item.getSaisonId()
                )
                .containsExactly(
                        "Division 1-2026",
                        "Division 1-2025",
                        "Division 2-2026"
                );
    }

    @Test
    void getTeamsReturnsTeamsByChampionnat() {
        FlaTeam team = new FlaTeam();
        team.setId(2L);
        team.setChampionnatId(101L);
        team.setSaisonId(2026L);
        team.setFlaTeamId(3001L);
        team.setTeamName("A Team");
        when(flaTeamRepository.findByChampionnatIdOrderByTeamNameAsc(101L)).thenReturn(List.of(team));

        var response = service().getTeams(101L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getFlaTeamId()).isEqualTo(3001L);
        assertThat(response.get(0).getTeamName()).isEqualTo("A Team");
        verify(flaTeamRepository).findByChampionnatIdOrderByTeamNameAsc(101L);
    }

    @Test
    void getTeamsReturnsEmptyListWhenNoTeamExists() {
        when(flaTeamRepository.findByChampionnatIdOrderByTeamNameAsc(101L))
                .thenReturn(List.of());

        var response = service().getTeams(101L);

        assertThat(response).isEmpty();

        verify(flaTeamRepository)
                .findByChampionnatIdOrderByTeamNameAsc(101L);
    }

    @Test
    void getTeamsMapsAllDisplayFields() {
        FlaTeam team = new FlaTeam();
        team.setId(2L);
        team.setChampionnatId(101L);
        team.setSaisonId(2026L);
        team.setFlaTeamId(3001L);
        team.setTeamName("A Team");

        when(flaTeamRepository.findByChampionnatIdOrderByTeamNameAsc(101L))
                .thenReturn(List.of(team));

        var response = service().getTeams(101L);

        assertThat(response).hasSize(1);

        var first = response.get(0);
        assertThat(first.getId()).isEqualTo(2L);
        assertThat(first.getChampionnatId()).isEqualTo(101L);
        assertThat(first.getSaisonId()).isEqualTo(2026L);
        assertThat(first.getFlaTeamId()).isEqualTo(3001L);
        assertThat(first.getTeamName()).isEqualTo("A Team");

        verify(flaTeamRepository)
                .findByChampionnatIdOrderByTeamNameAsc(101L);
    }

    @Test
    void getTeamsKeepsRepositoryOrder() {
        FlaTeam first = new FlaTeam();
        first.setId(1L);
        first.setChampionnatId(101L);
        first.setSaisonId(2026L);
        first.setFlaTeamId(3001L);
        first.setTeamName("A Team");

        FlaTeam second = new FlaTeam();
        second.setId(2L);
        second.setChampionnatId(101L);
        second.setSaisonId(2026L);
        second.setFlaTeamId(3002L);
        second.setTeamName("B Team");

        when(flaTeamRepository.findByChampionnatIdOrderByTeamNameAsc(101L))
                .thenReturn(List.of(first, second));

        var response = service().getTeams(101L);

        assertThat(response)
                .extracting(FlaTeamResponse::getTeamName)
                .containsExactly("A Team", "B Team");
    }

    @Test
    void getTeamsReturnsTeamsByChampionnatAndSeason() {
        FlaTeam team = new FlaTeam();
        team.setId(2L);
        team.setChampionnatId(101L);
        team.setSaisonId(2026L);
        team.setFlaTeamId(3001L);
        team.setTeamName("A Team");

        when(flaTeamRepository
                .findByChampionnatIdAndSaisonIdOrderByTeamNameAsc(101L, 2026L))
                .thenReturn(List.of(team));

        var response = service().getTeams(101L, 2026L);

        assertThat(response).hasSize(1);

        var first = response.get(0);
        assertThat(first.getId()).isEqualTo(2L);
        assertThat(first.getChampionnatId()).isEqualTo(101L);
        assertThat(first.getSaisonId()).isEqualTo(2026L);
        assertThat(first.getFlaTeamId()).isEqualTo(3001L);
        assertThat(first.getTeamName()).isEqualTo("A Team");

        verify(flaTeamRepository)
                .findByChampionnatIdAndSaisonIdOrderByTeamNameAsc(101L, 2026L);
    }

    @Test
    void getTeamsReturnsEmptyListWhenNoTeamExistsForChampionnatAndSeason() {
        when(flaTeamRepository
                .findByChampionnatIdAndSaisonIdOrderByTeamNameAsc(101L, 2026L))
                .thenReturn(List.of());

        var response = service().getTeams(101L, 2026L);

        assertThat(response).isEmpty();

        verify(flaTeamRepository)
                .findByChampionnatIdAndSaisonIdOrderByTeamNameAsc(101L, 2026L);
    }

    @Test
    void getTeamsByChampionnatIdAndSeasonIdKeepsRepositoryOrder() {
        FlaTeam first = new FlaTeam();
        first.setId(1L);
        first.setChampionnatId(101L);
        first.setSaisonId(2026L);
        first.setFlaTeamId(3001L);
        first.setTeamName("A Team");

        FlaTeam second = new FlaTeam();
        second.setId(2L);
        second.setChampionnatId(101L);
        second.setSaisonId(2026L);
        second.setFlaTeamId(3002L);
        second.setTeamName("B Team");

        when(flaTeamRepository
                .findByChampionnatIdAndSaisonIdOrderByTeamNameAsc(101L, 2026L))
                .thenReturn(List.of(first, second));

        var response = service().getTeams(101L, 2026L);

        assertThat(response)
                .extracting(FlaTeamResponse::getTeamName)
                .containsExactly("A Team", "B Team");
    }

    private FlaCacheServiceImpl service() {
        return new FlaCacheServiceImpl(flaChampionnatRepository, flaTeamRepository);
    }
}
