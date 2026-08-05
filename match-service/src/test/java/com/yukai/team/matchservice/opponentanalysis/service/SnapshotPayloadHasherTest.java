package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotPayloadHasherTest {

    private final SnapshotPayloadHasher hasher = new SnapshotPayloadHasher(new JacksonConfig().objectMapper());

    @Test
    void sameDataGetsSameHash() {
        String first = hasher.hash(List.of(entry(1L, 10, List.of("won", "lost")))).payloadHash();
        String second = hasher.hash(List.of(entry(1L, 10, List.of("won", "lost")))).payloadHash();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void inputTeamOrderDoesNotChangeHash() {
        String first = hasher.hash(List.of(
                entry(2L, 20, List.of("draw")),
                entry(1L, 10, List.of("won"))
        )).payloadHash();
        String second = hasher.hash(List.of(
                entry(1L, 10, List.of("won")),
                entry(2L, 20, List.of("draw"))
        )).payloadHash();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void statisticChangeChangesHash() {
        String first = hasher.hash(List.of(entry(1L, 10, List.of("won")))).payloadHash();
        String second = hasher.hash(List.of(entry(1L, 11, List.of("won")))).payloadHash();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void formOrderChangeChangesHash() {
        String first = hasher.hash(List.of(entry(1L, 10, List.of("won", "lost")))).payloadHash();
        String second = hasher.hash(List.of(entry(1L, 10, List.of("lost", "won")))).payloadHash();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashIsLowercaseSha256Hex() {
        String hash = hasher.hash(List.of(entry(1L, 10, List.of("won")))).payloadHash();

        assertThat(hash).matches("[0-9a-f]{64}");
    }

    private FlaStandingEntryResponse entry(Long teamId, Integer points, List<String> form) {
        FlaStandingEntryResponse entry = new FlaStandingEntryResponse();
        entry.setTeamId(teamId);
        entry.setTeamName("Team " + teamId);
        entry.setChampionnatId(1365L);
        entry.setPoints(points);
        entry.setWins(1);
        entry.setDraws(0);
        entry.setLosses(0);
        entry.setGoalsFor(3);
        entry.setGoalsAgainst(1);
        entry.setBonus(0);
        entry.setForfeits(0);
        entry.setForm(form);
        return entry;
    }
}
