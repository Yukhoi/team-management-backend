package com.yukai.team.auditservice.service;

import com.yukai.team.auditservice.dto.AuditLogResponse;
import com.yukai.team.auditservice.dto.PageResponse;
import com.yukai.team.auditservice.entity.OperationLog;
import com.yukai.team.auditservice.repository.OperationLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AuditQueryService {

    private final OperationLogRepository operationLogRepository;

    public AuditQueryService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getLogs(
            int page,
            int size,
            String eventType,
            String aggregateType,
            Long aggregateId,
            String username,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        var logs = operationLogRepository.findAll(
                logSpecification(eventType, aggregateType, aggregateId, username, from, to),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operatedAt"))
        );
        return new PageResponse<>(
                logs.getContent().stream().map(AuditLogResponse::from).toList(),
                logs.getNumber(),
                logs.getSize(),
                logs.getTotalElements(),
                logs.getTotalPages()
        );
    }

    private Specification<OperationLog> logSpecification(
            String eventType,
            String aggregateType,
            Long aggregateId,
            String username,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (hasText(eventType)) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), eventType.trim()));
            }
            if (hasText(aggregateType)) {
                predicates.add(criteriaBuilder.equal(root.get("bizType"), aggregateType.trim()));
            }
            if (aggregateId != null) {
                predicates.add(criteriaBuilder.equal(root.get("bizId"), aggregateId));
            }
            if (hasText(username)) {
                predicates.add(criteriaBuilder.equal(root.get("operatorUsername"), username.trim()));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("operatedAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("operatedAt"), to));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getLog(Long id) {
        OperationLog operationLog = operationLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Audit log not found"));
        return AuditLogResponse.from(operationLog);
    }
}
