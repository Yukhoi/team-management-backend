package com.yukai.team.matchservice.outbox;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import org.springframework.stereotype.Component;

@Component
public class OperatorProvider {

    private static final EventOperator SYSTEM_OPERATOR = new EventOperator(null, "system");

    public EventOperator currentOperator() {
        CurrentUser currentUser = UserContextHolder.get();
        if (currentUser == null) {
            return SYSTEM_OPERATOR;
        }

        String username = currentUser.username();
        if (username == null || username.isBlank()) {
            username = "system";
        }

        return new EventOperator(currentUser.userId(), username);
    }
}
