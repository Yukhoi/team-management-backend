package com.yukai.team.matchservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "match_appearance", schema = "match")
public class MatchAppearance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchInfo match;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "player_name_snapshot", nullable = false, length = 100)
    private String playerNameSnapshot;

    @Column(name = "jersey_number_snapshot")
    private Integer jerseyNumberSnapshot;

    @Column(name = "position_snapshot", length = 30)
    private String positionSnapshot;

    @Column(name = "appeared", nullable = false)
    private Boolean appeared;

    @Column(name = "starter", nullable = false)
    private Boolean starter;

    @Column(name = "on_minute")
    private Integer onMinute;

    @Column(name = "off_minute")
    private Integer offMinute;

    @Column(name = "remark")
    private String remark;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (appeared == null) {
            appeared = true;
        }
        if (starter == null) {
            starter = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
