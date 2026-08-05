package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.ExternalDataSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:external-data-snapshot;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE SCHEMA IF NOT EXISTS match",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=match"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ExternalDataSnapshotRepositoryTest.JpaTestConfig.class)
class ExternalDataSnapshotRepositoryTest {

    @Autowired
    private ExternalDataSnapshotRepository externalDataSnapshotRepository;

    @Test
    void findsLatestSnapshotForProviderChampionnatAndSaison() {
        externalDataSnapshotRepository.save(snapshot("FLA", 1365L, 15L, hash("a"), OffsetDateTime.parse("2026-08-05T08:00:00Z")));
        externalDataSnapshotRepository.save(snapshot("FLA", 1365L, 15L, hash("b"), OffsetDateTime.parse("2026-08-05T12:00:00Z")));

        var latest = externalDataSnapshotRepository
                .findTopByProviderAndChampionnatIdAndSaisonIdOrderByFetchedAtDescIdDesc("FLA", 1365L, 15L);

        assertThat(latest).isPresent();
        assertThat(latest.get().getPayloadHash()).isEqualTo(hash("b"));
    }

    @Test
    void providerChampionnatSaisonAndHashAreUniqueTogether() {
        externalDataSnapshotRepository.saveAndFlush(snapshot("FLA", 1365L, 15L, hash("a"), OffsetDateTime.parse("2026-08-05T08:00:00Z")));

        assertThatThrownBy(() -> externalDataSnapshotRepository.saveAndFlush(
                snapshot("FLA", 1365L, 15L, hash("a"), OffsetDateTime.parse("2026-08-05T09:00:00Z"))
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void differentSaisonCanSaveSameHash() {
        externalDataSnapshotRepository.saveAndFlush(snapshot("FLA", 1365L, 15L, hash("a"), OffsetDateTime.parse("2026-08-05T08:00:00Z")));
        externalDataSnapshotRepository.saveAndFlush(snapshot("FLA", 1365L, 16L, hash("a"), OffsetDateTime.parse("2026-08-05T09:00:00Z")));

        assertThat(externalDataSnapshotRepository.findAll()).hasSize(2);
    }

    @Test
    void differentChampionnatCanSaveSameHash() {
        externalDataSnapshotRepository.saveAndFlush(snapshot("FLA", 1365L, 15L, hash("a"), OffsetDateTime.parse("2026-08-05T08:00:00Z")));
        externalDataSnapshotRepository.saveAndFlush(snapshot("FLA", 1366L, 15L, hash("a"), OffsetDateTime.parse("2026-08-05T09:00:00Z")));

        assertThat(externalDataSnapshotRepository.findAll()).hasSize(2);
    }

    private ExternalDataSnapshot snapshot(
            String provider,
            Long championnatId,
            Long saisonId,
            String payloadHash,
            OffsetDateTime fetchedAt
    ) {
        ExternalDataSnapshot snapshot = new ExternalDataSnapshot();
        snapshot.setProvider(provider);
        snapshot.setChampionnatId(championnatId);
        snapshot.setSaisonId(saisonId);
        snapshot.setPayloadHash(payloadHash);
        snapshot.setRawPayload("[]");
        snapshot.setFetchedAt(fetchedAt);
        snapshot.setCreatedAt(fetchedAt);
        return snapshot;
    }

    private String hash(String prefix) {
        return (prefix + "123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef").substring(0, 64);
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ExternalDataSnapshot.class)
    @EnableJpaRepositories(basePackageClasses = ExternalDataSnapshotRepository.class)
    static class JpaTestConfig {
    }
}
