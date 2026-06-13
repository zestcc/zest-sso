package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.enums.UserStatus;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoAuditLog;
import cn.zest.sso.server.domain.entity.SsoOAuthClient;
import cn.zest.sso.server.domain.entity.SsoTenant;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.mapper.SsoAuditLogMapper;
import cn.zest.sso.server.domain.mapper.SsoOAuthClientMapper;
import cn.zest.sso.server.domain.mapper.SsoTenantMapper;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import cn.zest.sso.server.domain.vo.DashboardStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SsoUserMapper userMapper;
    private final SsoOAuthClientMapper clientMapper;
    private final SsoTenantMapper tenantMapper;
    private final SsoAuditLogMapper auditLogMapper;
    private final SsoProperties ssoProperties;

    public DashboardStatsVO getStats() {
        long totalUsers = userMapper.selectCount(null);
        long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<SsoUser>()
                .eq(SsoUser::getStatus, UserStatus.ACTIVE.getCode()));
        long totalClients = clientMapper.selectCount(null);
        long activeClients = clientMapper.selectCount(new LambdaQueryWrapper<SsoOAuthClient>()
                .eq(SsoOAuthClient::getStatus, 1));
        long totalTenants = tenantMapper.selectCount(null);

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long loginSuccess24h = auditLogMapper.selectCount(new LambdaQueryWrapper<SsoAuditLog>()
                .eq(SsoAuditLog::getEventType, AuditEventType.LOGIN_SUCCESS.getCode())
                .ge(SsoAuditLog::getCreateTime, since));
        long loginFailure24h = auditLogMapper.selectCount(new LambdaQueryWrapper<SsoAuditLog>()
                .eq(SsoAuditLog::getEventType, AuditEventType.LOGIN_FAILURE.getCode())
                .ge(SsoAuditLog::getCreateTime, since));

        List<SsoAuditLog> recentAuditLogs = auditLogMapper.selectList(new LambdaQueryWrapper<SsoAuditLog>()
                .orderByDesc(SsoAuditLog::getCreateTime)
                .last("LIMIT 10"));

        return DashboardStatsVO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalClients(totalClients)
                .activeClients(activeClients)
                .totalTenants(totalTenants)
                .loginSuccess24h(loginSuccess24h)
                .loginFailure24h(loginFailure24h)
                .issuer(ssoProperties.getIssuer())
                .recentAuditLogs(recentAuditLogs)
                .build();
    }
}
