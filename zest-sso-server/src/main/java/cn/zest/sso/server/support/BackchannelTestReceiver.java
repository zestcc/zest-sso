package cn.zest.sso.server.support;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内存记录最近一次 Back-Channel 测试 RP 收到的 logout_token 主体（仅联调/测试）。
 */
@Component
public class BackchannelTestReceiver {

    private final AtomicReference<Receipt> lastReceipt = new AtomicReference<>();

    public void record(String principalName, String clientId, String logoutToken) {
        lastReceipt.set(new Receipt(principalName, clientId, logoutToken, Instant.now()));
    }

    public Receipt lastReceipt() {
        return lastReceipt.get();
    }

    public void clear() {
        lastReceipt.set(null);
    }

    public record Receipt(String principalName, String clientId, String logoutToken, Instant receivedAt) {
    }
}
