package com.resolveiq.orchestration.application.action;

import com.resolveiq.orchestration.domain.model.action.ActionType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ActionRegistry {

    private final Map<ActionType, ResolutionAction<?, ?>> actionMap = new EnumMap<>(ActionType.class);

    public ActionRegistry(List<ResolutionAction<?, ?>> actions) {
        for (ResolutionAction<?, ?> action : actions) {
            actionMap.put(action.type(), action);
        }
    }

    @SuppressWarnings("unchecked")
    public <I, O> ResolutionAction<I, O> getAction(ActionType type) {
        ResolutionAction<?, ?> action = actionMap.get(type);
        if (action == null) {
            throw new IllegalArgumentException("Unsupported action type: " + type);
        }
        return (ResolutionAction<I, O>) action;
    }

    public boolean supports(ActionType type) {
        return actionMap.containsKey(type);
    }
}
