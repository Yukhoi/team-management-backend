package com.yukai.team.auditservice.service;

import com.yukai.team.auditservice.entity.OperationLog;
import com.yukai.team.auditservice.repository.OperationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    @Mock
    private OperationLogRepository operationLogRepository;

    @Test
    void getLogsReturnsNewestFirstPage() {
        OperationLog log = operationLog(1L);
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "operatedAt"));
        when(operationLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        var response = new AuditQueryService(operationLogRepository).getLogs(
                0, 20, null, null, null, null, null, null
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getLogsAcceptsEventAggregateAndIdFilters() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "operatedAt"));
        when(operationLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var response = new AuditQueryService(operationLogRepository).getLogs(
                0,
                20,
                "match.goal.created",
                "match",
                1L,
                "coach",
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-12-31T23:59:59Z")
        );

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    void getLogReturnsSelectedLog() {
        when(operationLogRepository.findById(1L)).thenReturn(Optional.of(operationLog(1L)));

        var response = new AuditQueryService(operationLogRepository).getLog(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getLogThrowsNotFoundWhenIdDoesNotExist() {
        when(operationLogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new AuditQueryService(operationLogRepository).getLog(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Audit log not found");
    }

    private OperationLog operationLog(Long id) {
        OperationLog log = new OperationLog();
        log.setId(id);
        log.setEventType("match.goal.created");
        log.setBizType("match");
        return log;
    }
}
