package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.client.FlaClient;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaSyncResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlaSyncServiceImplTest {

    private static final Long CHAMPIONNAT_ID = 1365L;
    private static final Long SAISON_ID = 15L;
    private static final String CHAMPIONNAT_NAME = "Division 3";

    @Mock
    private FlaClient flaClient;

    @Mock
    private FlaCacheWriter flaCacheWriter;

    @InjectMocks
    private FlaSyncServiceImpl service;

    @Test
    void validStandingsAreDelegatedToCacheWriter() {
        List<FlaStandingEntryResponse> standings = List.of(
                entry(6580L, "A Team"),
                entry(6581L, "B Team")
        );

        FlaSyncResponse expected = syncResponse(2, 0, 0);

        when(flaClient.getStandings(CHAMPIONNAT_ID, SAISON_ID))
                .thenReturn(standings);

        when(flaCacheWriter.upsertStandings(
                CHAMPIONNAT_ID,
                SAISON_ID,
                CHAMPIONNAT_NAME,
                standings
        )).thenReturn(expected);

        FlaSyncResponse actual = service.syncStandings(
                CHAMPIONNAT_ID,
                SAISON_ID,
                CHAMPIONNAT_NAME
        );

        assertThat(actual).isSameAs(expected);

        verify(flaClient).getStandings(
                CHAMPIONNAT_ID,
                SAISON_ID
        );

        verify(flaCacheWriter).upsertStandings(
                CHAMPIONNAT_ID,
                SAISON_ID,
                CHAMPIONNAT_NAME,
                standings
        );
    }

    @Test
    void emptyStandingsAreDelegatedToCacheWriter() {
        List<FlaStandingEntryResponse> standings = List.of();
        FlaSyncResponse expected = syncResponse(0, 0, 0);

        when(flaClient.getStandings(CHAMPIONNAT_ID, SAISON_ID))
                .thenReturn(standings);

        when(flaCacheWriter.upsertStandings(
                CHAMPIONNAT_ID,
                SAISON_ID,
                null,
                standings
        )).thenReturn(expected);

        FlaSyncResponse actual = service.syncStandings(
                CHAMPIONNAT_ID,
                SAISON_ID,
                null
        );

        assertThat(actual).isSameAs(expected);

        verify(flaCacheWriter).upsertStandings(
                CHAMPIONNAT_ID,
                SAISON_ID,
                null,
                standings
        );
    }

    @Test
    void mismatchedChampionnatFailsBeforeCallingWriter() {
        FlaStandingEntryResponse standing = entry(6580L, "A Team");
        standing.setChampionnatId(999L);

        when(flaClient.getStandings(CHAMPIONNAT_ID, SAISON_ID))
                .thenReturn(List.of(standing));

        assertThatThrownBy(() ->
                service.syncStandings(
                        CHAMPIONNAT_ID,
                        SAISON_ID,
                        CHAMPIONNAT_NAME
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FLA standing championnatId does not match request");

        verifyNoInteractions(flaCacheWriter);
    }

    @Test
    void nullStandingEntryFailsBeforeCallingWriter() {
        List<FlaStandingEntryResponse> standings = new ArrayList<>();
        standings.add(entry(6580L, "A Team"));
        standings.add(null);

        when(flaClient.getStandings(CHAMPIONNAT_ID, SAISON_ID))
                .thenReturn(standings);

        assertThatThrownBy(() ->
                service.syncStandings(
                        CHAMPIONNAT_ID,
                        SAISON_ID,
                        CHAMPIONNAT_NAME
                )
        )
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(flaCacheWriter);
    }

    @Test
    void nullChampionnatIdFailsWithoutCallingDependencies() {
        assertThatThrownBy(() ->
                service.syncStandings(
                        null,
                        SAISON_ID,
                        CHAMPIONNAT_NAME
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("championnatId");

        verifyNoInteractions(flaClient, flaCacheWriter);
    }

    @Test
    void zeroChampionnatIdFailsWithoutCallingDependencies() {
        assertThatThrownBy(() ->
                service.syncStandings(
                        0L,
                        SAISON_ID,
                        CHAMPIONNAT_NAME
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("championnatId");

        verifyNoInteractions(flaClient, flaCacheWriter);
    }

    @Test
    void negativeChampionnatIdFailsWithoutCallingDependencies() {
        assertThatThrownBy(() ->
                service.syncStandings(
                        -1L,
                        SAISON_ID,
                        CHAMPIONNAT_NAME
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("championnatId");

        verifyNoInteractions(flaClient, flaCacheWriter);
    }

    @Test
    void nullSaisonIdFailsWithoutCallingDependencies() {
        assertThatThrownBy(() ->
                service.syncStandings(
                        CHAMPIONNAT_ID,
                        null,
                        CHAMPIONNAT_NAME
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saisonId");

        verifyNoInteractions(flaClient, flaCacheWriter);
    }

    @Test
    void zeroSaisonIdFailsWithoutCallingDependencies() {
        assertThatThrownBy(() ->
                service.syncStandings(
                        CHAMPIONNAT_ID,
                        0L,
                        CHAMPIONNAT_NAME
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saisonId");

        verifyNoInteractions(flaClient, flaCacheWriter);
    }

    @Test
    void negativeSaisonIdFailsWithoutCallingDependencies() {
        assertThatThrownBy(() ->
                service.syncStandings(
                        CHAMPIONNAT_ID,
                        -1L,
                        CHAMPIONNAT_NAME
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saisonId");

        verifyNoInteractions(flaClient, flaCacheWriter);
    }

    @Test
    void flaClientFailureIsPropagatedAndWriterIsNotCalled() {
        RuntimeException exception =
                new RuntimeException("FLA service unavailable");

        when(flaClient.getStandings(CHAMPIONNAT_ID, SAISON_ID))
                .thenThrow(exception);

        assertThatThrownBy(() ->
                service.syncStandings(
                        CHAMPIONNAT_ID,
                        SAISON_ID,
                        CHAMPIONNAT_NAME
                )
        ).isSameAs(exception);

        verify(flaCacheWriter, never())
                .upsertStandings(
                        CHAMPIONNAT_ID,
                        SAISON_ID,
                        CHAMPIONNAT_NAME,
                        List.of()
                );
    }

    private FlaStandingEntryResponse entry(
            Long teamId,
            String teamName
    ) {
        FlaStandingEntryResponse response =
                new FlaStandingEntryResponse();

        response.setChampionnatId(CHAMPIONNAT_ID);
        response.setTeamId(teamId);
        response.setTeamName(teamName);

        return response;
    }

    private FlaSyncResponse syncResponse(
            int created,
            int updated,
            int unchanged
    ) {
        return new FlaSyncResponse(
                CHAMPIONNAT_ID,
                SAISON_ID,
                CHAMPIONNAT_NAME,
                created + updated + unchanged,
                created,
                updated,
                unchanged,
                OffsetDateTime.now()
        );
    }
}