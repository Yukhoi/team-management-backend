package com.yukai.team.matchservice.opponentanalysis.service;

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
        when(flaChampionnatRepository.findAllByOrderByNameAscSaisonIdDesc()).thenReturn(List.of(championnat));

        var response = service().getChampionnats();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getChampionnatId()).isEqualTo(101L);
        assertThat(response.get(0).getName()).isEqualTo("Division 1");
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

    private FlaCacheServiceImpl service() {
        return new FlaCacheServiceImpl(flaChampionnatRepository, flaTeamRepository);
    }
}
