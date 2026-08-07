package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.client.TeamServiceClient;
import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.client.dto.InternalTeamInfo;
import com.yukai.team.matchservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMappingResponse;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlaTeamMappingServiceImplTest {

    private static final Long TOURNAMENT_ID = 5L;
    private static final Long INTERNAL_TEAM_ID = 12L;
    private static final Long FLA_CHAMPIONNAT_ID = 1365L;
    private static final Long FLA_SAISON_ID = 15L;
    private static final Long FLA_TEAM_ID = 6580L;

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
    @InjectMocks
    private FlaTeamMappingServiceImpl service;
    @Captor
    private ArgumentCaptor<FlaTeamMapping> mappingCaptor;

    @Test
    void upsertMapping_shouldCreateNewMapping_whenMappingDoesNotExist() {
        // Given
        commonValidations();

        when(flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(
                        TOURNAMENT_ID,
                        INTERNAL_TEAM_ID
                ))
                .thenReturn(Optional.empty());

        when(flaTeamMappingRepository.save(any(FlaTeamMapping.class)))
                .thenAnswer(invocation -> {
                    FlaTeamMapping mapping = invocation.getArgument(0);
                    mapping.setId(1L);
                    return mapping;
                });

        // When
        FlaTeamMappingResponse response = service.upsertMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                request()
        );

        // Then
        verify(flaTeamMappingRepository).save(mappingCaptor.capture());

        FlaTeamMapping savedMapping = mappingCaptor.getValue();

        assertThat(savedMapping.getInternalTournamentId())
                .isEqualTo(TOURNAMENT_ID);
        assertThat(savedMapping.getInternalTeamId())
                .isEqualTo(INTERNAL_TEAM_ID);
        assertThat(savedMapping.getFlaChampionnatId())
                .isEqualTo(FLA_CHAMPIONNAT_ID);
        assertThat(savedMapping.getFlaSaisonId())
                .isEqualTo(FLA_SAISON_ID);
        assertThat(savedMapping.getFlaTeamId())
                .isEqualTo(FLA_TEAM_ID);
        assertThat(savedMapping.getFlaTeamName())
                .isEqualTo("FLA Team");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getInternalTournamentId())
                .isEqualTo(TOURNAMENT_ID);
        assertThat(response.getInternalTeamId())
                .isEqualTo(INTERNAL_TEAM_ID);
        assertThat(response.getInternalTeamName())
                .isEqualTo("Internal Team");
        assertThat(response.getFlaChampionnatId())
                .isEqualTo(FLA_CHAMPIONNAT_ID);
        assertThat(response.getFlaSaisonId())
                .isEqualTo(FLA_SAISON_ID);
        assertThat(response.getFlaTeamId())
                .isEqualTo(FLA_TEAM_ID);
        assertThat(response.getFlaTeamName())
                .isEqualTo("FLA Team");

        verifyCommonValidationCalls();
        verifyNoMoreInteractions(
                tournamentClient,
                teamServiceClient,
                flaChampionnatRepository,
                flaTeamRepository,
                flaTeamMappingRepository
        );
    }

    @Test
    void upsertMapping_shouldUpdateExistingMapping_andPreserveCreatedAt() {
        // Given
        OffsetDateTime originalCreatedAt =
                OffsetDateTime.parse("2026-08-04T20:00:00Z");

        FlaTeamMapping existing = mapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                1365L,
                15L,
                7000L,
                "Old FLA Team"
        );
        existing.setId(1L);
        existing.setCreatedAt(originalCreatedAt);

        commonValidations();

        when(flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(
                        TOURNAMENT_ID,
                        INTERNAL_TEAM_ID
                ))
                .thenReturn(Optional.of(existing));

        when(flaTeamMappingRepository.save(existing))
                .thenReturn(existing);

        // When
        FlaTeamMappingResponse response = service.upsertMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                request()
        );

        // Then
        verify(flaTeamMappingRepository).save(mappingCaptor.capture());

        FlaTeamMapping savedMapping = mappingCaptor.getValue();

        assertThat(savedMapping).isSameAs(existing);
        assertThat(savedMapping.getCreatedAt())
                .isEqualTo(originalCreatedAt);

        assertThat(savedMapping.getInternalTournamentId())
                .isEqualTo(TOURNAMENT_ID);
        assertThat(savedMapping.getInternalTeamId())
                .isEqualTo(INTERNAL_TEAM_ID);
        assertThat(savedMapping.getFlaChampionnatId())
                .isEqualTo(FLA_CHAMPIONNAT_ID);
        assertThat(savedMapping.getFlaSaisonId())
                .isEqualTo(FLA_SAISON_ID);
        assertThat(savedMapping.getFlaTeamId())
                .isEqualTo(FLA_TEAM_ID);
        assertThat(savedMapping.getFlaTeamName())
                .isEqualTo("FLA Team");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFlaTeamId())
                .isEqualTo(FLA_TEAM_ID);
        assertThat(response.getFlaTeamName())
                .isEqualTo("FLA Team");
        assertThat(response.getCreatedAt())
                .isEqualTo(originalCreatedAt);

        verifyCommonValidationCalls();
    }

    @Test
    void upsertMapping_shouldAllowUpdatingMappingToItsCurrentFlaTeam() {
        // Given
        FlaTeamMapping existing = mapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                FLA_CHAMPIONNAT_ID,
                FLA_SAISON_ID,
                FLA_TEAM_ID,
                "FLA Team"
        );
        existing.setId(1L);

        commonValidations();

        when(flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(
                        TOURNAMENT_ID,
                        INTERNAL_TEAM_ID
                ))
                .thenReturn(Optional.of(existing));

        when(flaTeamMappingRepository.save(existing))
                .thenReturn(existing);

        // When
        FlaTeamMappingResponse response = service.upsertMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                request()
        );

        // Then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFlaTeamId()).isEqualTo(FLA_TEAM_ID);

        verify(flaTeamMappingRepository)
                .existsByInternalTournamentIdAndFlaChampionnatIdAndFlaSaisonIdAndFlaTeamIdAndInternalTeamIdNot(
                        TOURNAMENT_ID,
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID,
                        FLA_TEAM_ID,
                        INTERNAL_TEAM_ID
                );

        verify(flaTeamMappingRepository).save(existing);
    }

    @Test
    void upsertMapping_shouldFail_whenFlaTeamDoesNotExist() {
        // Given
        mockValidTournament();
        mockValidInternalTeam();
        mockValidFlaChampionnat();

        when(flaTeamRepository
                .findByChampionnatIdAndSaisonIdAndFlaTeamId(
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID,
                        FLA_TEAM_ID
                ))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.upsertMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                request()
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FLA team not found");

        verify(flaTeamMappingRepository, never()).save(any());
        verify(flaTeamMappingRepository, never())
                .existsByInternalTournamentIdAndFlaChampionnatIdAndFlaSaisonIdAndFlaTeamIdAndInternalTeamIdNot(
                        anyLong(),
                        anyLong(),
                        anyLong(),
                        anyLong(),
                        anyLong()
                );
    }

    @Test
    void upsertMapping_shouldFail_whenFlaChampionnatDoesNotExist() {
        // Given
        mockValidTournament();
        mockValidInternalTeam();

        when(flaChampionnatRepository
                .findByChampionnatIdAndSaisonId(
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID
                ))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.upsertMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                request()
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FLA championnat not found");

        verifyNoInteractions(flaTeamRepository);
        verify(flaTeamMappingRepository, never()).save(any());
    }

    @Test
    void upsertMapping_shouldFail_whenTournamentDoesNotExist() {
        // Given
        when(tournamentClient.getTournamentSnapshot(TOURNAMENT_ID))
                .thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> service.upsertMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                request()
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Tournament not found");

        verifyNoInteractions(
                teamServiceClient,
                flaChampionnatRepository,
                flaTeamRepository,
                flaTeamMappingRepository
        );
    }

    @Test
    void upsertMapping_shouldFail_whenInternalTeamDoesNotExist() {
        // Given
        mockValidTournament();

        when(teamServiceClient.getTeam(INTERNAL_TEAM_ID))
                .thenThrow(new EntityNotFoundException("Team not found"));

        // When / Then
        assertThatThrownBy(() -> service.upsertMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                request()
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Team not found");

        verifyNoInteractions(
                flaChampionnatRepository,
                flaTeamRepository,
                flaTeamMappingRepository
        );
    }

    @Test
    void upsertMapping_shouldFail_whenFlaTeamIsMappedToAnotherInternalTeam() {
        // Given
        commonValidations();

        when(flaTeamMappingRepository
                .existsByInternalTournamentIdAndFlaChampionnatIdAndFlaSaisonIdAndFlaTeamIdAndInternalTeamIdNot(
                        TOURNAMENT_ID,
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID,
                        FLA_TEAM_ID,
                        INTERNAL_TEAM_ID
                ))
                .thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> service.upsertMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                request()
        ))
                .isInstanceOf(FlaMappingConflictException.class)
                .hasMessage(
                        "FLA team is already mapped in this tournament"
                );

        verify(flaTeamMappingRepository, never())
                .findByInternalTournamentIdAndInternalTeamId(
                        anyLong(),
                        anyLong()
                );
        verify(flaTeamMappingRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void upsertMapping_shouldFail_whenTournamentIdIsInvalid(
            Long invalidTournamentId
    ) {
        assertThatThrownBy(() -> service.upsertMapping(
                invalidTournamentId,
                INTERNAL_TEAM_ID,
                request()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tournamentId");

        verifyNoInteractions(
                tournamentClient,
                teamServiceClient,
                flaChampionnatRepository,
                flaTeamRepository,
                flaTeamMappingRepository
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void upsertMapping_shouldFail_whenTeamIdIsInvalid(Long invalidTeamId) {
        assertThatThrownBy(() -> service.upsertMapping(
                TOURNAMENT_ID,
                invalidTeamId,
                request()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teamId");

        verifyNoInteractions(
                tournamentClient,
                teamServiceClient,
                flaChampionnatRepository,
                flaTeamRepository,
                flaTeamMappingRepository
        );
    }

    @Test
    void upsertMapping_shouldPropagatePersistenceFailure() {
        // Given
        commonValidations();

        when(flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(
                        TOURNAMENT_ID,
                        INTERNAL_TEAM_ID
                ))
                .thenReturn(Optional.empty());

        when(flaTeamMappingRepository.save(any(FlaTeamMapping.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Mapping constraint violation"
                ));

        // When / Then
        assertThatThrownBy(() -> service.upsertMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                request()
        ))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Mapping constraint violation");

        verify(flaTeamMappingRepository).save(any(FlaTeamMapping.class));
    }

    private void commonValidations() {
        mockValidTournament();
        mockValidInternalTeam();
        mockValidFlaChampionnat();
        mockValidFlaTeam();

        when(flaTeamMappingRepository
                .existsByInternalTournamentIdAndFlaChampionnatIdAndFlaSaisonIdAndFlaTeamIdAndInternalTeamIdNot(
                        TOURNAMENT_ID,
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID,
                        FLA_TEAM_ID,
                        INTERNAL_TEAM_ID
                ))
                .thenReturn(false);
    }

    private void mockValidTournament() {
        TournamentSnapshotResponse tournament =
                new TournamentSnapshotResponse();
        tournament.setId(TOURNAMENT_ID);

        when(tournamentClient.getTournamentSnapshot(TOURNAMENT_ID))
                .thenReturn(tournament);
    }

    private void mockValidInternalTeam() {
        when(teamServiceClient.getTeam(INTERNAL_TEAM_ID))
                .thenReturn(team(INTERNAL_TEAM_ID, "Internal Team"));
    }

    private void mockValidFlaChampionnat() {
        FlaChampionnat championnat = new FlaChampionnat();
        championnat.setChampionnatId(FLA_CHAMPIONNAT_ID);
        championnat.setSaisonId(FLA_SAISON_ID);

        when(flaChampionnatRepository
                .findByChampionnatIdAndSaisonId(
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID
                ))
                .thenReturn(Optional.of(championnat));
    }

    private void mockValidFlaTeam() {
        FlaTeam flaTeam = new FlaTeam();
        flaTeam.setChampionnatId(FLA_CHAMPIONNAT_ID);
        flaTeam.setSaisonId(FLA_SAISON_ID);
        flaTeam.setFlaTeamId(FLA_TEAM_ID);
        flaTeam.setTeamName("FLA Team");

        when(flaTeamRepository
                .findByChampionnatIdAndSaisonIdAndFlaTeamId(
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID,
                        FLA_TEAM_ID
                ))
                .thenReturn(Optional.of(flaTeam));
    }

    private void verifyCommonValidationCalls() {
        verify(tournamentClient)
                .getTournamentSnapshot(TOURNAMENT_ID);
        verify(teamServiceClient)
                .getTeam(INTERNAL_TEAM_ID);
        verify(flaChampionnatRepository)
                .findByChampionnatIdAndSaisonId(
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID
                );
        verify(flaTeamRepository)
                .findByChampionnatIdAndSaisonIdAndFlaTeamId(
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID,
                        FLA_TEAM_ID
                );
        verify(flaTeamMappingRepository)
                .existsByInternalTournamentIdAndFlaChampionnatIdAndFlaSaisonIdAndFlaTeamIdAndInternalTeamIdNot(
                        TOURNAMENT_ID,
                        FLA_CHAMPIONNAT_ID,
                        FLA_SAISON_ID,
                        FLA_TEAM_ID,
                        INTERNAL_TEAM_ID
                );
        verify(flaTeamMappingRepository)
                .findByInternalTournamentIdAndInternalTeamId(
                        TOURNAMENT_ID,
                        INTERNAL_TEAM_ID
                );
    }

    @Test
    void getMappingsShouldReturnSortedMappings() {
        mockValidTournament();

        FlaTeamMapping mapping1 = mapping(
                5L,
                2L,
                1365L,
                15L,
                6580L,
                "FLA Team B"
        );

        FlaTeamMapping mapping2 = mapping(
                5L,
                1L,
                1365L,
                15L,
                6581L,
                "FLA Team A"
        );

        when(flaTeamMappingRepository
                .findByInternalTournamentIdOrderByInternalTeamIdAsc(5L))
                .thenReturn(List.of(mapping1, mapping2));

        when(teamServiceClient.getTeam(1L))
                .thenReturn(team(1L, "Arsenal"));

        when(teamServiceClient.getTeam(2L))
                .thenReturn(team(2L, "Chelsea"));

        List<FlaTeamMappingResponse> responses =
                service.getMappings(5L);

        assertThat(responses)
                .hasSize(2);

        // 最终按 InternalTeamName 排序
        assertThat(responses.get(0).getInternalTeamName())
                .isEqualTo("Arsenal");

        assertThat(responses.get(1).getInternalTeamName())
                .isEqualTo("Chelsea");

        verify(flaTeamMappingRepository)
                .findByInternalTournamentIdOrderByInternalTeamIdAsc(5L);

        verify(teamServiceClient).getTeam(1L);
        verify(teamServiceClient).getTeam(2L);
    }

    @Test
    void getMappingsShouldReturnEmptyList() {

        mockValidTournament();

        when(flaTeamMappingRepository
                .findByInternalTournamentIdOrderByInternalTeamIdAsc(5L))
                .thenReturn(List.of());

        List<FlaTeamMappingResponse> responses =
                service.getMappings(5L);

        assertThat(responses).isEmpty();

        verifyNoInteractions(teamServiceClient);
    }

    @Test
    void getMappingsShouldFailWhenTournamentDoesNotExist() {

        when(tournamentClient.getTournamentSnapshot(5L))
                .thenReturn(null);

        assertThatThrownBy(() -> service.getMappings(5L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Tournament not found");

        verifyNoInteractions(
                flaTeamMappingRepository,
                teamServiceClient
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void getMappingsShouldFailWhenTournamentIdInvalid(Long id) {

        assertThatThrownBy(() -> service.getMappings(id))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(
                tournamentClient,
                flaTeamMappingRepository,
                teamServiceClient
        );
    }

    @Test
    void getMappingShouldReturnResponse() {

        mockValidTournament();

        when(teamServiceClient.getTeam(12L))
                .thenReturn(team(12L, "Internal Team"));

        FlaTeamMapping mapping = mapping(
                5L,
                12L,
                1365L,
                15L,
                6580L,
                "FLA Team"
        );

        when(flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.of(mapping));

        FlaTeamMappingResponse response =
                service.getMapping(5L, 12L);

        assertThat(response.getInternalTeamName())
                .isEqualTo("Internal Team");

        assertThat(response.getFlaTeamName())
                .isEqualTo("FLA Team");

        verify(teamServiceClient).getTeam(12L);

        verify(flaTeamMappingRepository)
                .findByInternalTournamentIdAndInternalTeamId(5L, 12L);
    }

    @Test
    void getMappingShouldFailWhenMappingNotFound() {

        mockValidTournament();

        when(teamServiceClient.getTeam(12L))
                .thenReturn(team(12L, "Internal Team"));

        when(flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(5L, 12L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getMapping(5L, 12L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FLA team mapping not found");
    }

    @Test
    void getMappingShouldFailWhenInternalTeamDoesNotExist() {

        mockValidTournament();

        when(teamServiceClient.getTeam(12L))
                .thenThrow(new EntityNotFoundException("Team not found"));

        assertThatThrownBy(() ->
                service.getMapping(5L, 12L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Team not found");
    }

    @Test
    void deleteMapping_shouldDeleteExistingMapping() {
        // Given
        mockValidTournament();
        mockValidInternalTeam();

        FlaTeamMapping existing = mapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID,
                FLA_CHAMPIONNAT_ID,
                FLA_SAISON_ID,
                FLA_TEAM_ID,
                "FLA Team"
        );
        existing.setId(1L);

        when(flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(
                        TOURNAMENT_ID,
                        INTERNAL_TEAM_ID
                ))
                .thenReturn(Optional.of(existing));

        // When
        service.deleteMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID
        );

        // Then
        verify(tournamentClient)
                .getTournamentSnapshot(TOURNAMENT_ID);

        verify(teamServiceClient)
                .getTeam(INTERNAL_TEAM_ID);

        verify(flaTeamMappingRepository)
                .findByInternalTournamentIdAndInternalTeamId(
                        TOURNAMENT_ID,
                        INTERNAL_TEAM_ID
                );

        verify(flaTeamMappingRepository)
                .delete(existing);

        verifyNoMoreInteractions(
                tournamentClient,
                teamServiceClient,
                flaTeamMappingRepository
        );
    }

    @Test
    void deleteMapping_shouldFail_whenMappingDoesNotExist() {
        // Given
        mockValidTournament();
        mockValidInternalTeam();

        when(flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(
                        TOURNAMENT_ID,
                        INTERNAL_TEAM_ID
                ))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.deleteMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("FLA team mapping not found");

        verify(flaTeamMappingRepository, never())
                .delete(any(FlaTeamMapping.class));
    }

    @Test
    void deleteMapping_shouldFail_whenTournamentDoesNotExist() {
        // Given
        when(tournamentClient.getTournamentSnapshot(TOURNAMENT_ID))
                .thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> service.deleteMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Tournament not found");

        verifyNoInteractions(
                teamServiceClient,
                flaTeamMappingRepository
        );
    }

    @Test
    void deleteMapping_shouldFail_whenInternalTeamDoesNotExist() {
        // Given
        mockValidTournament();

        when(teamServiceClient.getTeam(INTERNAL_TEAM_ID))
                .thenThrow(new EntityNotFoundException("Team not found"));

        // When / Then
        assertThatThrownBy(() -> service.deleteMapping(
                TOURNAMENT_ID,
                INTERNAL_TEAM_ID
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Team not found");

        verifyNoInteractions(flaTeamMappingRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void deleteMapping_shouldFail_whenTournamentIdIsInvalid(
            Long invalidTournamentId
    ) {
        // When / Then
        assertThatThrownBy(() -> service.deleteMapping(
                invalidTournamentId,
                INTERNAL_TEAM_ID
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tournamentId");

        verifyNoInteractions(
                tournamentClient,
                teamServiceClient,
                flaTeamMappingRepository
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void deleteMapping_shouldFail_whenTeamIdIsInvalid(
            Long invalidTeamId
    ) {
        // When / Then
        assertThatThrownBy(() -> service.deleteMapping(
                TOURNAMENT_ID,
                invalidTeamId
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teamId");

        verifyNoInteractions(
                tournamentClient,
                teamServiceClient,
                flaTeamMappingRepository
        );
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
