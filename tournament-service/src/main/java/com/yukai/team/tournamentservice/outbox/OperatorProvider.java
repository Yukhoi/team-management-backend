package com.yukai.team.tournamentservice.outbox;

import com.yukai.team.tournamentservice.context.CurrentUser;
import com.yukai.team.tournamentservice.context.UserContextHolder;
import org.springframework.stereotype.Component;

@Component
public class OperatorProvider {

    private static final String SYSTEM_USERNAME = "system";

    public EventOperator currentOperator() {
        CurrentUser currentUser = UserContextHolder.get();
        if (currentUser == null) {
            return new EventOperator(null, SYSTEM_USERNAME);
        }
        return new EventOperator(currentUser.userId(), currentUser.username());
    }
}
