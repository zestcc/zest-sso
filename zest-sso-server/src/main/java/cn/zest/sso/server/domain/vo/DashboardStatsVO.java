package cn.zest.sso.server.domain.vo;

import cn.zest.sso.server.domain.entity.SsoAuditLog;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardStatsVO {

    private long totalUsers;
    private long activeUsers;
    private long totalClients;
    private long activeClients;
    private long totalTenants;
    private long loginSuccess24h;
    private long loginFailure24h;
    private String issuer;
    private List<SsoAuditLog> recentAuditLogs;
}
