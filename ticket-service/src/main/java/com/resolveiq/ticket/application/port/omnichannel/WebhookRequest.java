package com.resolveiq.ticket.application.port.omnichannel;

import java.util.Map;

public record WebhookRequest(
    Map<String, String> headers,
    byte[] rawBody,
    String requestUri
) {}
