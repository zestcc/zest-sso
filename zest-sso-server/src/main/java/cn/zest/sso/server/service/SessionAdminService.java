package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.vo.SessionVO;
import cn.zest.sso.server.support.AdminAuditSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SessionAdminService {

    private final SessionRepository<? extends Session> sessionRepository;
    private final StringRedisTemplate redisTemplate;
    private final AdminAuditSupport auditSupport;

    @Value("${spring.session.redis.namespace:zest-sso:session}")
    private String sessionNamespace;

    public List<SessionVO> listSessions(String username) {
        Set<String> sessionIds = new HashSet<>();
        if (username != null && !username.isBlank()
                && sessionRepository instanceof FindByIndexNameSessionRepository<?> indexed) {
            Map<String, ? extends Session> sessions = indexed.findByPrincipalName(username);
            sessionIds.addAll(sessions.keySet());
        } else {
            sessionIds.addAll(scanSessionIds());
        }

        List<SessionVO> result = new ArrayList<>();
        for (String sessionId : sessionIds) {
            Session session = sessionRepository.findById(sessionId);
            if (session != null) {
                result.add(toSessionVO(session));
            }
        }
        result.sort(Comparator.comparing(SessionVO::getLastAccessedTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public void revokeAllByUsername(String username) {
        if (username == null || username.isBlank()
                || !(sessionRepository instanceof FindByIndexNameSessionRepository<?> indexed)) {
            return;
        }
        Map<String, ? extends Session> sessions = indexed.findByPrincipalName(username);
        for (String sessionId : sessions.keySet()) {
            sessionRepository.deleteById(sessionId);
        }
        if (!sessions.isEmpty()) {
            auditSupport.log(AuditEventType.SESSION_REVOKE, username,
                    "登出联动：清除 " + sessions.size() + " 个会话");
        }
    }

    public void revokeSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId);
        if (session == null) {
            return;
        }
        String username = session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
        sessionRepository.deleteById(sessionId);
        auditSupport.log(AuditEventType.SESSION_REVOKE, username != null ? username : sessionId, sessionId);
    }

    private Set<String> scanSessionIds() {
        Set<String> sessionIds = new HashSet<>();
        String pattern = sessionNamespace + ":sessions:*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                int idx = key.lastIndexOf(':');
                if (idx > 0 && idx < key.length() - 1) {
                    sessionIds.add(key.substring(idx + 1));
                }
            }
        }
        return sessionIds;
    }

    private SessionVO toSessionVO(Session session) {
        String username = session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME);
        Instant creationTime = session.getCreationTime();
        Instant lastAccessed = session.getLastAccessedTime();
        long maxInactive = session.getMaxInactiveInterval().getSeconds();
        boolean expired = lastAccessed.plusSeconds(maxInactive).isBefore(Instant.now());
        return SessionVO.builder()
                .sessionId(session.getId())
                .username(username)
                .creationTime(creationTime)
                .lastAccessedTime(lastAccessed)
                .maxInactiveIntervalSeconds(maxInactive)
                .expired(expired)
                .build();
    }
}
