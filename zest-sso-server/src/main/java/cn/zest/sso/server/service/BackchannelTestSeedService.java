package cn.zest.sso.server.service;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;

/**
 * 为 Back-Channel 联调在 oauth2_authorization 中植入最小授权记录。
 */
@Service
@RequiredArgsConstructor
public class BackchannelTestSeedService {

    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;

    public void seedAdminAuthorization(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "clientId 不能为空");
        }
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new SsoException(ErrorCode.CLIENT_NOT_FOUND, "OAuth 客户端不存在: " + clientId);
        }
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(UUID.randomUUID().toString())
                .principalName("admin")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of("openid", "profile"))
                .build();
        authorizationService.save(authorization);
    }
}
