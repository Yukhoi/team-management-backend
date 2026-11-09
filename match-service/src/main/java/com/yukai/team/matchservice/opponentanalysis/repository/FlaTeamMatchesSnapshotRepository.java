package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMatchesSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface FlaTeamMatchesSnapshotRepository extends JpaRepository<FlaTeamMatchesSnapshot, String> {
    Optional<FlaTeamMatchesSnapshot> findTopByFlaTeamIdOrderByFetchedAtDescPayloadHashDesc(Long teamId);

    @Modifying(clearAutomatically = true)
    @Query("update FlaTeamMatchesSnapshot s set s.fetchedAt = :at where s.payloadHash = :hash and s.fetchedAt < :at")
    int advanceFetchedAt(@Param("hash") String hash, @Param("at") OffsetDateTime at);
}
