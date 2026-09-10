package com.resolveiq.security.flags;

import com.resolveiq.contracts.flags.FeatureFlagStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public class JdbcFeatureFlagStore implements FeatureFlagStore {
    private static final Logger log = LoggerFactory.getLogger(JdbcFeatureFlagStore.class);
    private final DataSource dataSource;
    private volatile boolean initialized = false;

    public JdbcFeatureFlagStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void ensureTable() {
        if (!initialized && dataSource != null) {
            synchronized (this) {
                if (!initialized) {
                    try (Connection conn = dataSource.getConnection();
                         Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE TABLE IF NOT EXISTS tenant_feature_flags (" +
                                "tenant_id UUID NOT NULL, " +
                                "flag_key VARCHAR(100) NOT NULL, " +
                                "enabled BOOLEAN NOT NULL, " +
                                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                "PRIMARY KEY(tenant_id, flag_key))");
                        initialized = true;
                    } catch (Exception e) {
                        log.warn("Could not ensure tenant_feature_flags table: {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public Optional<Boolean> getFlag(UUID tenantId, String flagKey) {
        if (dataSource == null || tenantId == null || flagKey == null) {
            return Optional.empty();
        }
        ensureTable();
        String sql = "SELECT enabled FROM tenant_feature_flags WHERE tenant_id = ? AND flag_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, tenantId);
            ps.setString(2, flagKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getBoolean("enabled"));
                }
            }
        } catch (SQLException e) {
            log.warn("Error reading feature flag from database for tenant {}: {}", tenantId, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void setFlag(UUID tenantId, String flagKey, boolean enabled) {
        if (dataSource == null || tenantId == null || flagKey == null) {
            return;
        }
        ensureTable();
        String sql = "INSERT INTO tenant_feature_flags (tenant_id, flag_key, enabled, updated_at) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (tenant_id, flag_key) DO UPDATE SET enabled = EXCLUDED.enabled, updated_at = CURRENT_TIMESTAMP";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, tenantId);
            ps.setString(2, flagKey);
            ps.setBoolean(3, enabled);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("Error persisting feature flag to database for tenant {}: {}", tenantId, e.getMessage());
        }
    }
}
