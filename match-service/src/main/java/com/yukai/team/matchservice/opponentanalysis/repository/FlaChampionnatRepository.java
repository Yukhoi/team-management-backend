package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.FlaChampionnat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FlaChampionnatRepository extends JpaRepository<FlaChampionnat, Long>, JpaSpecificationExecutor<FlaChampionnat> {

    List<FlaChampionnat> findAllByOrderByNameAscSaisonIdDesc();

    Optional<FlaChampionnat> findByChampionnatIdAndSaisonId(Long championnatId, Long saisonId);

    List<FlaChampionnat> findByChampionnatIdOrderBySaisonIdDesc(Long championnatId);
}
