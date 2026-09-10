package com.resolveiq.security;

public enum Permission {
    INCIDENT_APPROVE,
    INCIDENT_PUBLISH,
    ACTION_APPROVE_LOW_RISK,
    ACTION_APPROVE_FINANCIAL,
    EVIDENCE_VIEW_ORIGINAL,
    CONVERSATION_MERGE,
    TICKET_ASSIGN,
    KNOWLEDGE_RELEASE_APPROVE;

    public String permissionName() {
        return name();
    }
}
