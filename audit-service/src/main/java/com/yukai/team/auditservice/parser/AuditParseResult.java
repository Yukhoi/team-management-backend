package com.yukai.team.auditservice.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditParseResult {

    private String beforeData;
    private String afterData;
}
