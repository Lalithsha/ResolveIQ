package com.resolveiq.ticket.adapter.in.web.omnichannel;

import com.resolveiq.ticket.application.dto.omnichannel.OmnichannelDtos.InboundWebhookResult;
import com.resolveiq.ticket.application.port.omnichannel.WebhookRequest;
import com.resolveiq.ticket.application.service.omnichannel.OmnichannelService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/v1/email")
public class EmailWebhookController {

    private static final Logger log = LoggerFactory.getLogger(EmailWebhookController.class);
    private final OmnichannelService omnichannelService;

    public EmailWebhookController(OmnichannelService omnichannelService) {
        this.omnichannelService = omnichannelService;
    }

    @PostMapping(value = "/{tenantPublicKey}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> receiveSignedEmail(
        @PathVariable String tenantPublicKey,
        @RequestBody byte[] rawBody,
        HttpServletRequest servletRequest
    ) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = servletRequest.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, servletRequest.getHeader(name));
            }
        }

        try {
            WebhookRequest request = new WebhookRequest(headers, rawBody, servletRequest.getRequestURI());
            InboundWebhookResult result = omnichannelService.processInboundEmailWebhook(tenantPublicKey, request);
            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            log.warn("Unauthorized webhook request rejected: reason={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Unauthorized",
                "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Bad webhook payload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Bad Request",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Internal error processing email webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", e.getMessage()
            ));
        }
    }
}
