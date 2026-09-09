package com.resolveiq.orchestration.application.action;

import com.resolveiq.orchestration.domain.model.action.ActionType;

public interface ResolutionAction<I, O> {
    ActionType type();
    Class<I> inputType();
    String extractTarget(I input);
    CurrentState fetchCurrentState(ActionContext context, I input);
    ValidationResult validate(ActionContext context, I input, CurrentState state);
    ExecutionResult execute(ActionContext context, I input, String idempotencyKey);
    ReconciliationResult reconcile(ActionContext context, I input, ExecutionResult executionResult);
    CompensationResult compensate(ActionContext context, I input, ExecutionResult executionResult);
}
