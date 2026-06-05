package com.yukai.team.auditservice.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DlqMessage {

    private String originalMessage;
    private String errorMessage;
    private String sourceTopic;
    private String sourceService;
    private OffsetDateTime failedAt;
}
