package com.yukai.team.matchservice.config;

import com.yukai.team.matchservice.service.TournamentReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.tournament-sync.reconcile", havingValue = "true")
public class TournamentReconciliationCommand implements ApplicationRunner {
    private final TournamentReconciliationService reconciliation;
    private final ConfigurableApplicationContext context;
    @Value("${app.tournament-sync.reconcile-only:false}")
    private boolean reconcileOnly;

    @Override
    public void run(ApplicationArguments args) {
        reconciliation.reconcileAll();
        if (reconcileOnly) context.close();
    }
}
