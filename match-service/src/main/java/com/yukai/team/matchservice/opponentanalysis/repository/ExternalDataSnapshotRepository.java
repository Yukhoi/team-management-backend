package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.ExternalDataSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ExternalDataSnapshotRepository extends JpaRepository<ExternalDataSnapshot, Long>, JpaSpecificationExecutor<ExternalDataSnapshot> {

    Optional<ExternalDataSnapshot> findByPayloadHash(String payloadHash);

    Optional<ExternalDataSnapshot> findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc(
            String provider,
            Long championnatId,
            Long saisonId
    );
}
