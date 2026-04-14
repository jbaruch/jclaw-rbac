package com.codepocalypse.rbac;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

/**
 * Tools available ONLY to the ADMIN role.
 *
 * These are sensitive operations -- listing users and reading system metrics.
 * @PreAuthorize("hasRole('ADMIN')") blocks any non-admin user at the method
 * level, even if the LLM tries to invoke the tool.
 */
@Component
public class AdminTools {

    private final InMemoryUserDetailsManager userDetailsManager;
    private final MetricsEndpoint metricsEndpoint;

    public AdminTools(InMemoryUserDetailsManager userDetailsManager,
                      MetricsEndpoint metricsEndpoint) {
        this.userDetailsManager = userDetailsManager;
        this.metricsEndpoint = metricsEndpoint;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Tool(description = "List all configured users in the system with their roles. Admin only.")
    public String listUsers() {
        var sb = new StringBuilder("Configured users:\n\n");
        for (String username : new String[]{"elvis", "spock", "godfather", "pirate", "yoda"}) {
            var user = userDetailsManager.loadUserByUsername(username);
            sb.append("- ").append(user.getUsername())
                    .append(" | roles: ").append(user.getAuthorities())
                    .append("\n");
        }
        return sb.toString();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Tool(description = "Introspect system metrics: JVM memory, uptime, HTTP request stats. Admin only.")
    public String introspect() {
        var sb = new StringBuilder("System Metrics:\n\n");

        appendMetric(sb, "jvm.memory.used", "JVM Memory Used");
        appendMetric(sb, "jvm.memory.max", "JVM Memory Max");
        appendMetric(sb, "process.uptime", "Process Uptime (seconds)");
        appendMetric(sb, "system.cpu.count", "CPU Cores");
        appendMetric(sb, "http.server.requests", "HTTP Requests");

        return sb.toString();
    }

    private void appendMetric(StringBuilder sb, String metricName, String label) {
        try {
            var response = metricsEndpoint.metric(metricName, null);
            if (response != null && !response.getMeasurements().isEmpty()) {
                double value = response.getMeasurements().getFirst().getValue();
                sb.append("- ").append(label).append(": ").append(String.format("%.2f", value)).append("\n");
            }
        } catch (Exception e) {
            sb.append("- ").append(label).append(": unavailable\n");
        }
    }
}
