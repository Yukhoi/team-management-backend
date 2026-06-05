package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.dto.AssistResponse;
import com.yukai.team.matchservice.dto.CreateGoalRequest;
import com.yukai.team.matchservice.dto.GoalResponse;
import com.yukai.team.matchservice.dto.UpdateGoalRequest;
import com.yukai.team.matchservice.dto.UpsertAssistRequest;

import java.util.List;

public interface GoalService {

    GoalResponse createGoal(Long matchId, CreateGoalRequest request);

    GoalResponse updateGoal(Long goalId, UpdateGoalRequest request);

    List<GoalResponse> getGoals(Long matchId);

    void deleteGoal(Long goalId);

    AssistResponse upsertAssist(Long goalId, UpsertAssistRequest request);

    void deleteAssist(Long goalId);
}
