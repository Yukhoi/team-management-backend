package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.FlaChampionnat;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fla-cache;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE SCHEMA IF NOT EXISTS match",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=match"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FlaCacheRepositoryTest.JpaTestConfig.class)
class FlaCacheRepositoryTest {

    @Autowired
    private FlaChampionnatRepository flaChampionnatRepository;

    @Autowired
    private FlaTeamRepository flaTeamRepository;

    @Test
    void championnatQueriesUseChampionnatAndSaison() {
        flaChampionnatRepository.save(championnat(101L, 2026L, "Division 1"));

        var result = flaChampionnatRepository.findByChampionnatIdAndSaisonId(101L, 2026L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Division 1");
    }

    @Test
    void teamQueriesUseChampionnatAndFlaTeamId() {
        flaTeamRepository.save(team(101L, 2026L, 3002L, "B Team"));
        flaTeamRepository.save(team(101L, 2026L, 3001L, "A Team"));

        assertThat(flaTeamRepository.findByChampionnatIdOrderByTeamNameAsc(101L))
                .extracting(FlaTeam::getTeamName)
                .containsExactly("A Team", "B Team");
        assertThat(flaTeamRepository.findByFlaTeamIdOrderByTeamNameAsc(3001L)).hasSize(1);
    }

    @Test
    void uniqueConstraintsAreEnforced() {
        flaChampionnatRepository.saveAndFlush(championnat(101L, 2026L, "Division 1"));
        flaTeamRepository.saveAndFlush(team(101L, 2026L, 3001L, "A Team"));

        assertThatThrownBy(() -> flaChampionnatRepository.saveAndFlush(championnat(101L, 2026L, "Duplicate")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> flaTeamRepository.saveAndFlush(team(101L, 2026L, 3001L, "Duplicate")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private FlaChampionnat championnat(Long championnatId, Long saisonId, String name) {
        FlaChampionnat championnat = new FlaChampionnat();
        championnat.setChampionnatId(championnatId);
        championnat.setSaisonId(saisonId);
        championnat.setName(name);
        return championnat;
    }

    private FlaTeam team(Long championnatId, Long saisonId, Long flaTeamId, String teamName) {
        FlaTeam team = new FlaTeam();
        team.setChampionnatId(championnatId);
        team.setSaisonId(saisonId);
        team.setFlaTeamId(flaTeamId);
        team.setTeamName(teamName);
        return team;
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {FlaChampionnat.class, FlaTeam.class})
    @EnableJpaRepositories(basePackageClasses = {FlaChampionnatRepository.class, FlaTeamRepository.class})
    static class JpaTestConfig {
    }
}
