package com.resolveiq.ticket.adapter.out.telemetry;

import com.resolveiq.ticket.application.port.SignalAdapter;
import com.resolveiq.ticket.domain.model.OperationalSignal;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SimulatedSignalAdapter implements SignalAdapter {

    @Override
    public List<OperationalSignal> fetchSignalsForWindow(UUID tenantId, String component, Instant windowStart, Instant windowEnd) {
        List<OperationalSignal> signals = new ArrayList<>();

        if (component != null && component.toLowerCase().contains("checkout")) {
            signals.add(new OperationalSignal(
                "DEPLOYMENT_EVENT",
                "deploy-checkout-v2.14.1",
                component,
                "production",
                "Canary release v2.14.1 enabled payment gateway timeout tuning",
                windowStart.plusSeconds(60),
                Map.of("commit", "8f3b41c", "author", "deploy-bot", "changeType", "config_update"),
                0.88
            ));
            signals.add(new OperationalSignal(
                "MONITORING_ALERT",
                "mon-p99-latency-spike",
                component,
                "production",
                "Checkout payment gateway p99 latency crossed 4500ms threshold",
                windowStart.plusSeconds(180),
                Map.of("metric", "payment.gateway.latency.p99", "severity", "CRITICAL"),
                0.94
            ));
        } else if (component != null && component.toLowerCase().contains("sso")) {
            signals.add(new OperationalSignal(
                "CERTIFICATE_LIFECYCLE",
                "cert-saml-signing-expiry",
                component,
                "production",
                "Fictional SAML signing certificate rotation failed to propagate to token issuer",
                windowStart.plusSeconds(120),
                Map.of("certSubject", "CN=sso.resolveiq.local", "error", "SAML_SIGNATURE_INVALID"),
                0.96
            ));
        }

        return signals;
    }
}
