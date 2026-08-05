package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisReport;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OpponentAnalysisReportRepository extends JpaRepository<OpponentAnalysisReport, Long>, JpaSpecificationExecutor<OpponentAnalysisReport> {

    Optional<OpponentAnalysisReport> findTopByMatchIdOrderByCreatedAtDescIdDesc(Long matchId);

    Optional<OpponentAnalysisReport> findTopByMatchIdAndOpponentTeamIdOrderByCreatedAtDescIdDesc(Long matchId, Long opponentTeamId);

    List<OpponentAnalysisReport> findByStatusOrderByCreatedAtAsc(OpponentAnalysisStatus status);
}
