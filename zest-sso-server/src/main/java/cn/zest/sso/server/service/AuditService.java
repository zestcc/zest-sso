package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.domain.entity.SsoAuditLog;
import cn.zest.sso.server.domain.mapper.SsoAuditLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志服务，异步写入避免阻塞主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final SsoAuditLogMapper auditLogMapper;

    @Async
    public void log(AuditEventType eventType, String actor, String target,
                    String clientId, String ipAddress, String userAgent, String detail) {
        try {
            SsoAuditLog auditLog = new SsoAuditLog();
            auditLog.setEventType(eventType.getCode());
            auditLog.setActor(actor);
            auditLog.setTarget(target);
            auditLog.setClientId(clientId);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setDetail(detail);
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("审计日志写入失败: event={}, actor={}", eventType.getCode(), actor, e);
        }
    }

    public Page<SsoAuditLog> page(int page, int size, String eventType, String actor,
                                  String startTime, String endTime) {
        LambdaQueryWrapper<SsoAuditLog> wrapper = buildWrapper(eventType, actor, startTime, endTime);
        wrapper.orderByDesc(SsoAuditLog::getCreateTime);
        return auditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<SsoAuditLog> export(String eventType, String actor, String startTime, String endTime, int limit) {
        LambdaQueryWrapper<SsoAuditLog> wrapper = buildWrapper(eventType, actor, startTime, endTime);
        wrapper.orderByDesc(SsoAuditLog::getCreateTime).last("LIMIT " + Math.min(limit, 10000));
        return auditLogMapper.selectList(wrapper);
    }

    public int purgeOlderThanDays(int retentionDays) {
        if (retentionDays <= 0) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return auditLogMapper.delete(new LambdaQueryWrapper<SsoAuditLog>()
                .lt(SsoAuditLog::getCreateTime, cutoff));
    }

    private LambdaQueryWrapper<SsoAuditLog> buildWrapper(String eventType, String actor,
                                                         String startTime, String endTime) {
        LambdaQueryWrapper<SsoAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (eventType != null && !eventType.isBlank()) {
            wrapper.eq(SsoAuditLog::getEventType, eventType);
        }
        if (actor != null && !actor.isBlank()) {
            wrapper.like(SsoAuditLog::getActor, actor);
        }
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(SsoAuditLog::getCreateTime, startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(SsoAuditLog::getCreateTime, endTime);
        }
        return wrapper;
    }
}
