package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMapping;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fla-team-mapping;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE SCHEMA IF NOT EXISTS match",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=match"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FlaTeamMappingRepositoryTest.JpaTestConfig.class)
class FlaTeamMappingRepositoryTest {

    @Autowired
    private FlaTeamMappingRepository flaTeamMappingRepository;

    @Test
    void internalTournamentAndTeamAreUnique() {
        flaTeamMappingRepository.saveAndFlush(mapping(5L, 12L, 1365L, 15L, 6580L));

        assertThatThrownBy(() -> flaTeamMappingRepository.saveAndFlush(mapping(5L, 12L, 1365L, 15L, 6581L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void externalTeamIsUniqueWithinInternalTournament() {
        flaTeamMappingRepository.saveAndFlush(mapping(5L, 12L, 1365L, 15L, 6580L));

        assertThatThrownBy(() -> flaTeamMappingRepository.saveAndFlush(mapping(5L, 13L, 1365L, 15L, 6580L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void queryMethodsFindMappings() {
        flaTeamMappingRepository.save(mapping(5L, 12L, 1365L, 15L, 6580L));
        flaTeamMappingRepository.save(mapping(5L, 13L, 1365L, 15L, 6581L));

        assertThat(flaTeamMappingRepository.findByInternalTournamentIdAndInternalTeamId(5L, 12L)).isPresent();
        assertThat(flaTeamMappingRepository.findByInternalTournamentIdOrderByInternalTeamIdAsc(5L))
                .extracting(FlaTeamMapping::getInternalTeamId)
                .containsExactly(12L, 13L);
        assertThat(flaTeamMappingRepository.existsByInternalTournamentIdAndFlaChampionnatIdAndFlaSaisonIdAndFlaTeamIdAndInternalTeamIdNot(
                5L,
                1365L,
                15L,
                6580L,
                99L
        )).isTrue();
    }

    private FlaTeamMapping mapping(Long tournamentId, Long teamId, Long championnatId, Long saisonId, Long flaTeamId) {
        FlaTeamMapping mapping = new FlaTeamMapping();
        mapping.setInternalTournamentId(tournamentId);
        mapping.setInternalTeamId(teamId);
        mapping.setFlaChampionnatId(championnatId);
        mapping.setFlaSaisonId(saisonId);
        mapping.setFlaTeamId(flaTeamId);
        mapping.setFlaTeamName("FLA Team " + flaTeamId);
        return mapping;
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = FlaTeamMapping.class)
    @EnableJpaRepositories(basePackageClasses = FlaTeamMappingRepository.class)
    static class JpaTestConfig {
    }
}
