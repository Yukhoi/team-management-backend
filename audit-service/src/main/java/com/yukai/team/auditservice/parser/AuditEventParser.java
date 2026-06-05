package com.yukai.team.auditservice.parser;

import com.yukai.team.auditservice.dto.AuditEventMessage;

public interface AuditEventParser {

    boolean supports(String eventType);

    AuditParseResult parse(AuditEventMessage message);
}
