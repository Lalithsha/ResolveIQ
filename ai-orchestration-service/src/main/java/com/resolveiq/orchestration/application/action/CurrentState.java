package com.resolveiq.orchestration.application.action;

import java.util.Map;

public record CurrentState(
        boolean exists,
        String stateVersion,
        Map<String, Object> attributes
) {
    public static CurrentState notFound() {
        return new CurrentState(false, "0", Map.of());
    }

    public static CurrentState found(String stateVersion, Map<String, Object> attributes) {
        return new CurrentState(true, stateVersion, attributes);
    }
}
