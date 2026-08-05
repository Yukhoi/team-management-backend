package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.exception.SnapshotProcessingException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Component
public class SnapshotPayloadHasher {

    private final ObjectMapper objectMapper;

    public SnapshotPayloadHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SnapshotPayload hash(List<FlaStandingEntryResponse> standings) {
        try {
            String stableJson = stableJson(standings);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(digest.digest(stableJson.getBytes(StandardCharsets.UTF_8)));
            return new SnapshotPayload(stableJson, hash);
        } catch (JsonProcessingException exception) {
            throw new SnapshotProcessingException("Failed to serialize FLA standings snapshot", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new SnapshotProcessingException("SHA-256 is not available", exception);
        }
    }

    private String stableJson(List<FlaStandingEntryResponse> standings) throws JsonProcessingException {
        List<FlaStandingEntryResponse> sorted = safeStandings(standings).stream()
                .sorted(Comparator
                        .comparing((FlaStandingEntryResponse entry) -> entry.getTeamId(), Comparator.nullsLast(Long::compareTo))
                        .thenComparing(entry -> entry.getTeamName() == null ? "" : entry.getTeamName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return objectMapper.writeValueAsString(sorted);
    }

    private List<FlaStandingEntryResponse> safeStandings(List<FlaStandingEntryResponse> standings) {
        if (standings == null) {
            return List.of();
        }
        return standings.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    public record SnapshotPayload(String rawPayload, String payloadHash) {
    }
}
