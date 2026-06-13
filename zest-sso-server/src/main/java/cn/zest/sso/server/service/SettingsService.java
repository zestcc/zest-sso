package cn.zest.sso.server.service;

import cn.zest.sso.common.constant.SsoConstants;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.vo.SettingsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SsoProperties ssoProperties;

    public SettingsVO getSettings() {
        return SettingsVO.builder()
                .issuer(ssoProperties.getIssuer())
                .keyId(ssoProperties.getJwt().getKeyId())
                .accessTokenTtl(ssoProperties.getToken().getAccessTokenTtl())
                .refreshTokenTtl(ssoProperties.getToken().getRefreshTokenTtl())
                .idTokenTtl(ssoProperties.getToken().getIdTokenTtl())
                .loginRateLimit(ssoProperties.getSecurity().getLoginRateLimit())
                .loginRateWindowSeconds(ssoProperties.getSecurity().getLoginRateWindowSeconds())
                .maxLoginAttempts(SsoConstants.MAX_LOGIN_ATTEMPTS)
                .loginLockMinutes(SsoConstants.LOGIN_LOCK_MINUTES)
                .adminConsolePath("/admin/")
                .build();
    }
}
