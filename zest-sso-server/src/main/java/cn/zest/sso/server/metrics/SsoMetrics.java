package cn.zest.sso.server.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * SSO 业务指标（对标 Keycloak/Okta 运维可观测性）。
 */
@Component
public class SsoMetrics {

    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final Counter logout;
    private final Counter passwordResetRequest;
    private final Counter tokenRevoked;
    private final Counter backchannelLogoutSuccess;
    private final Counter backchannelLogoutFailure;

    public SsoMetrics(MeterRegistry registry) {
        this.loginSuccess = Counter.builder("zest.sso.login.success").register(registry);
        this.loginFailure = Counter.builder("zest.sso.login.failure").register(registry);
        this.logout = Counter.builder("zest.sso.logout").register(registry);
        this.passwordResetRequest = Counter.builder("zest.sso.password.reset.request").register(registry);
        this.tokenRevoked = Counter.builder("zest.sso.token.revoked").register(registry);
        this.backchannelLogoutSuccess = Counter.builder("zest.sso.backchannel.logout.success").register(registry);
        this.backchannelLogoutFailure = Counter.builder("zest.sso.backchannel.logout.failure").register(registry);
    }

    public void recordLoginSuccess() {
        loginSuccess.increment();
    }

    public void recordLoginFailure() {
        loginFailure.increment();
    }

    public void recordLogout() {
        logout.increment();
    }

    public void recordPasswordResetRequest() {
        passwordResetRequest.increment();
    }

    public void recordTokenRevoked() {
        tokenRevoked.increment();
    }

    public void recordBackchannelLogoutSuccess() {
        backchannelLogoutSuccess.increment();
    }

    public void recordBackchannelLogoutFailure() {
        backchannelLogoutFailure.increment();
    }
}
