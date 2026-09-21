package com.resolveiq.orchestration.application.action;

/**
 * Raised when an approval or execution is attempted after a proposal's
 * short-lived authorization window has closed.
 */
public class ActionProposalExpiredException extends IllegalStateException {

    public ActionProposalExpiredException(String message) {
        super(message);
    }
}
