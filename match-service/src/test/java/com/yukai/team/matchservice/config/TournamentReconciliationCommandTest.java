package com.yukai.team.matchservice.config;

import com.yukai.team.matchservice.service.TournamentReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TournamentReconciliationCommandTest {
    @Test
    void repairIsOptInAndOneShotClosesOnlyAfterSuccessfulCompletion() {
        var condition = TournamentReconciliationCommand.class.getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class);
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isFalse();
        var service = mock(TournamentReconciliationService.class);
        var context = mock(ConfigurableApplicationContext.class);
        var command = new TournamentReconciliationCommand(service,context);
        ReflectionTestUtils.setField(command,"reconcileOnly",true);
        command.run(mock(ApplicationArguments.class));
        var order = inOrder(service,context);
        order.verify(service).reconcileAll(); order.verify(context).close();
    }
    @Test
    void repairFailurePropagatesToTheCommandInsteadOfReportingSuccess() {
        var service = mock(TournamentReconciliationService.class);
        var context = mock(ConfigurableApplicationContext.class);
        var command = new TournamentReconciliationCommand(service,context);
        doThrow(new IllegalStateException("authoritative snapshot unavailable")).when(service).reconcileAll();
        assertThatThrownBy(() -> command.run(mock(ApplicationArguments.class))).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(context);
    }
}
