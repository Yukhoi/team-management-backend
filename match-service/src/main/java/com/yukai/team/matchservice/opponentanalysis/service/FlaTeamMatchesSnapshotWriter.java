package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMatchesSnapshot;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamMatchesSnapshotRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/** Separate transactions ensure a concurrent unique-key failure is rolled back before reloading. */
@Repository
public class FlaTeamMatchesSnapshotWriter {
    private final EntityManager entityManager;
    private final FlaTeamMatchesSnapshotRepository repository;

    public FlaTeamMatchesSnapshotWriter(EntityManager entityManager, FlaTeamMatchesSnapshotRepository repository) {
        this.entityManager = entityManager;
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(FlaTeamMatchesSnapshot snapshot) {
        entityManager.persist(snapshot);
        // Flush at transaction commit so JpaTransactionManager translates constraint failures
        // and completes rollback before the caller attempts to reload a concurrent winner.
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FlaTeamMatchesSnapshot touch(String hash, OffsetDateTime fetchedAt) {
        repository.advanceFetchedAt(hash, fetchedAt);
        return repository.findById(hash).orElseThrow();
    }
}
