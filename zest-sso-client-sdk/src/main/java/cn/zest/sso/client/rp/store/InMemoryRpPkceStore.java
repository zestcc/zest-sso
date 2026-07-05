package cn.zest.sso.client.rp.store;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 PKCE 存储 — 单机/开发默认实现。
 */
public class InMemoryRpPkceStore implements RpPkceStore {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void save(String state, String codeVerifier) {
        store.put(state, new Entry(codeVerifier, Instant.now().plus(TTL)));
    }

    @Override
    public String consume(String state) {
        Entry entry = store.remove(state);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return entry.codeVerifier();
    }

    private record Entry(String codeVerifier, Instant expiresAt) {
    }
}
