package com.yukai.team.matchservice.repository;

import com.yukai.team.matchservice.entity.MatchInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MatchInfoRepository extends JpaRepository<MatchInfo, Long>, JpaSpecificationExecutor<MatchInfo> {

    Page<MatchInfo> findAllByOrderByMatchTimeDesc(Pageable pageable);

    Page<MatchInfo> findByTournamentIdOrderByMatchTimeDesc(Long tournamentId, Pageable pageable);
}
