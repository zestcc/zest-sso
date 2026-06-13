package cn.zest.sso.server.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 测试环境 Mock Redis 配置。
 */
@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(template.opsForSet()).thenReturn(setOps);
        when(setOps.isMember(anyString(), anyString())).thenReturn(false);
        when(ops.increment(anyString())).thenReturn(1L);
        when(template.hasKey(anyString())).thenReturn(false);
        return template;
    }

    @Bean
    @Primary
    public SessionRepository<Session> sessionRepository() {
        Map<String, Session> sessions = new ConcurrentHashMap<>();
        return new SessionRepository<>() {
            @Override
            public Session createSession() {
                MapSession session = new MapSession();
                sessions.put(session.getId(), session);
                return session;
            }

            @Override
            public void save(Session session) {
                sessions.put(session.getId(), session);
            }

            @Override
            public Session findById(String id) {
                return sessions.get(id);
            }

            @Override
            public void deleteById(String id) {
                sessions.remove(id);
            }
        };
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }
}
