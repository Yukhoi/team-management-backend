package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.client.TeamServiceClient;
import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.client.dto.InternalTeamInfo;
import com.yukai.team.matchservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.UpsertFlaTeamMappingRequest;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaChampionnat;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeam;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMapping;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaMappingConflictException;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaChampionnatRepository;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamMappingRepository;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlaTeamMappingServiceImplTest {

    @Mock
    private FlaTeamMappingRepository flaTeamMappingRepository;
    @Mock
    private FlaChampionnatRepository flaChampionnatRepository;
    @Mock
    private FlaTeamRepository flaTeamRepository;
    @Mock
    private TeamServiceClient teamServiceClient;
    @Mock
    private TournamentClient tournamentClient;

    @Test
    void upsertCreatesMapping() {
        commonValidations();
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L)).thenReturn(Optional.empty());
        when(flaTeamMappingRepository.save(any(FlaTeamMapping.class))).thenAnswer(invocation -> {
            FlaTeamMapping mapping = invocation.getArgument(0);
            mapping.setId(1L);
            return mapping;
        });

        var response = service().upsertMapping(5L, 12L, request());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getInternalTeamName()).isEqualTo("Internal Team");
        assertThat(response.getFlaTeamName()).isEqualTo("FLA Team");
    }

    @Test
    void upsertUpdatesExistingMappingAndPreservesCreatedAt() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-04T20:00:00Z");
        FlaTeamMapping existing = mapping(5L, 12L, 1365L, 15L, 7000L, "Old FLA Team");
        existing.setId(1L);
        existing.setCreatedAt(createdAt);
        commonValidations();
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L)).thenReturn(Optional.of(existing));
        when(flaTeamMappingRepository.save(existing)).thenReturn(existing);

        var response = service().upsertMapping(5L, 12L, request());

        assertThat(existing.getCreatedAt()).isEqualTo(createdAt);
        assertThat(existing.getFlaTeamId()).isEqualTo(6580L);
        assertThat(existing.getFlaTeamName()).isEqualTo("FLA Team");
        assertThat(response.getFlaTeamId()).isEqualTo(6580L);
    }

    @Test
    void upsertFailsWhenFlaTeamDoesNotExist() {
        when(tournamentClient.getTournamentSnapshot(5L)).thenReturn(new TournamentSnapshotResponse());
        when(teamServiceClient.getTeam(12L)).thenReturn(team(12L, "Internal Team"));
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.of(new FlaChampionnat()));
        when(flaTeamRepository.findByChampionnatIdAndSaisonIdAndFlaTeamId(1365L, 15L, 6580L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().upsertMapping(5L, 12L, request()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FLA team not found");
    }

    @Test
    void upsertFailsWhenFlaChampionnatDoesNotExist() {
        when(tournamentClient.getTournamentSnapshot(5L)).thenReturn(new TournamentSnapshotResponse());
        when(teamServiceClient.getTeam(12L)).thenReturn(team(12L, "Internal Team"));
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().upsertMapping(5L, 12L, request()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FLA championnat not found");
    }

    @Test
    void upsertFailsWhenTournamentDoesNotExist() {
        when(tournamentClient.getTournamentSnapshot(5L)).thenReturn(null);

        assertThatThrownBy(() -> service().upsertMapping(5L, 12L, request()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void upsertFailsWhenInternalTeamDoesNotExist() {
        when(tournamentClient.getTournamentSnapshot(5L)).thenReturn(new TournamentSnapshotResponse());
        when(teamServiceClient.getTeam(12L)).thenThrow(new EntityNotFoundException("Team not found"));

        assertThatThrownBy(() -> service().upsertMapping(5L, 12L, request()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Team not found");
    }

    @Test
    void upsertFailsWhenFlaTeamIsOccupiedByAnotherInternalTeam() {
        commonValidations();
        when(flaTeamMappingRepository.existsByInternalTournamentIdAndFlaChampionnatIdAndFlaSaisonIdAndFlaTeamIdAndInternalTeamIdNot(
                5L,
                1365L,
                15L,
                6580L,
                12L
        )).thenReturn(true);

        assertThatThrownBy(() -> service().upsertMapping(5L, 12L, request()))
                .isInstanceOf(FlaMappingConflictException.class)
                .hasMessage("FLA team is already mapped in this tournament");
    }

    @Test
    void getMappingsReturnsAllSortedByInternalTeamName() {
        when(tournamentClient.getTournamentSnapshot(5L)).thenReturn(new TournamentSnapshotResponse());
        when(flaTeamMappingRepository.findByInternalTournamentIdOrderByInternalTeamIdAsc(5L))
                .thenReturn(List.of(
                        mapping(5L, 12L, 1365L, 15L, 6580L, "FLA B"),
                        mapping(5L, 11L, 1365L, 15L, 6579L, "FLA A")
                ));
        when(teamServiceClient.getTeam(12L)).thenReturn(team(12L, "B Team"));
        when(teamServiceClient.getTeam(11L)).thenReturn(team(11L, "A Team"));

        var response = service().getMappings(5L);

        assertThat(response).extracting("internalTeamName").containsExactly("A Team", "B Team");
    }

    @Test
    void getMappingReturnsOne() {
        FlaTeamMapping mapping = mapping(5L, 12L, 1365L, 15L, 6580L, "FLA Team");
        when(tournamentClient.getTournamentSnapshot(5L)).thenReturn(new TournamentSnapshotResponse());
        when(teamServiceClient.getTeam(12L)).thenReturn(team(12L, "Internal Team"));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L)).thenReturn(Optional.of(mapping));

        var response = service().getMapping(5L, 12L);

        assertThat(response.getInternalTeamId()).isEqualTo(12L);
        assertThat(response.getFlaTeamId()).isEqualTo(6580L);
    }

    @Test
    void deleteMappingDeletesExistingMapping() {
        FlaTeamMapping mapping = mapping(5L, 12L, 1365L, 15L, 6580L, "FLA Team");
        when(tournamentClient.getTournamentSnapshot(5L)).thenReturn(new TournamentSnapshotResponse());
        when(teamServiceClient.getTeam(12L)).thenReturn(team(12L, "Internal Team"));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L)).thenReturn(Optional.of(mapping));

        service().deleteMapping(5L, 12L);

        verify(flaTeamMappingRepository).delete(mapping);
    }

    @Test
    void deleteMappingFailsWhenMissing() {
        when(tournamentClient.getTournamentSnapshot(5L)).thenReturn(new TournamentSnapshotResponse());
        when(teamServiceClient.getTeam(12L)).thenReturn(team(12L, "Internal Team"));
        when(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().deleteMapping(5L, 12L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FLA team mapping not found");
    }

    private void commonValidations() {
        when(tournamentClient.getTournamentSnapshot(5L)).thenReturn(new TournamentSnapshotResponse());
        when(teamServiceClient.getTeam(12L)).thenReturn(team(12L, "Internal Team"));
        when(flaChampionnatRepository.findByChampionnatIdAndSaisonId(1365L, 15L)).thenReturn(Optional.of(new FlaChampionnat()));
        when(flaTeamRepository.findByChampionnatIdAndSaisonIdAndFlaTeamId(1365L, 15L, 6580L))
                .thenReturn(Optional.of(flaTeam()));
    }

    private FlaTeamMappingServiceImpl service() {
        return new FlaTeamMappingServiceImpl(
                flaTeamMappingRepository,
                flaChampionnatRepository,
                flaTeamRepository,
                teamServiceClient,
                tournamentClient
        );
    }

    private UpsertFlaTeamMappingRequest request() {
        UpsertFlaTeamMappingRequest request = new UpsertFlaTeamMappingRequest();
        request.setFlaChampionnatId(1365L);
        request.setFlaSaisonId(15L);
        request.setFlaTeamId(6580L);
        return request;
    }

    private InternalTeamInfo team(Long id, String name) {
        InternalTeamInfo team = new InternalTeamInfo();
        team.setId(id);
        team.setName(name);
        team.setIsOurTeam(false);
        return team;
    }

    private FlaTeam flaTeam() {
        FlaTeam team = new FlaTeam();
        team.setChampionnatId(1365L);
        team.setSaisonId(15L);
        team.setFlaTeamId(6580L);
        team.setTeamName("FLA Team");
        return team;
    }

    private FlaTeamMapping mapping(
            Long tournamentId,
            Long teamId,
            Long flaChampionnatId,
            Long flaSaisonId,
            Long flaTeamId,
            String flaTeamName
    ) {
        FlaTeamMapping mapping = new FlaTeamMapping();
        mapping.setInternalTournamentId(tournamentId);
        mapping.setInternalTeamId(teamId);
        mapping.setFlaChampionnatId(flaChampionnatId);
        mapping.setFlaSaisonId(flaSaisonId);
        mapping.setFlaTeamId(flaTeamId);
        mapping.setFlaTeamName(flaTeamName);
        return mapping;
    }
}
