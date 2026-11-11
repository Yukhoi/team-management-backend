package com.yukai.team.matchservice.repository;

import com.yukai.team.matchservice.entity.MatchInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MatchInfoRepository extends JpaRepository<MatchInfo, Long>, JpaSpecificationExecutor<MatchInfo> {

    Page<MatchInfo> findAllByOrderByMatchTimeDesc(Pageable pageable);

    Page<MatchInfo> findByTournamentIdOrderByMatchTimeDesc(Long tournamentId, Pageable pageable);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE MatchInfo m SET m.tournamentNameSnapshot=:name, m.seasonSnapshot=:season, "
            + "m.updatedAt=:now, m.version=m.version+1 WHERE m.tournamentId=:tournamentId "
            + "AND (m.tournamentNameSnapshot<>:name OR m.seasonSnapshot<>:season)")
    int updateTournamentSnapshots(@Param("tournamentId") Long tournamentId, @Param("name") String name,
                                  @Param("season") String season, @Param("now") OffsetDateTime now);

    @Query("SELECT DISTINCT m.tournamentId FROM MatchInfo m WHERE m.tournamentId>:after ORDER BY m.tournamentId")
    List<Long> findTournamentIdsAfter(@Param("after") Long after, Pageable pageable);
}
