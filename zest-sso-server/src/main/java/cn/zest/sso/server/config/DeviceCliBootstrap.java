package cn.zest.sso.server.config;

import cn.zest.sso.server.domain.entity.SsoOAuthClient;
import cn.zest.sso.server.domain.mapper.SsoOAuthClientMapper;
import cn.zest.sso.server.security.DatabaseRegisteredClientRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 确保 Device Authorization 演示客户端存在（开发/验收环境）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCliBootstrap implements ApplicationRunner {

    private final SsoOAuthClientMapper clientMapper;
    private final DatabaseRegisteredClientRepository clientRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (clientRepository.findByClientId("device-cli") != null) {
            return;
        }
        SsoOAuthClient client = new SsoOAuthClient();
        client.setClientId("device-cli");
        client.setClientSecretHash("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi");
        client.setClientName("Device CLI Demo");
        client.setClientAuthenticationMethods("none");
        client.setAuthorizationGrantTypes("urn:ietf:params:oauth:grant-type:device_code,refresh_token");
        client.setRedirectUris("urn:ietf:wg:oauth:2.0:oob");
        client.setScopes("openid,profile");
        client.setRequirePkce(0);
        client.setRequireConsent(0);
        client.setAccessTokenTtl(3600);
        client.setRefreshTokenTtl(86400);
        client.setStatus(1);
        clientMapper.insert(client);
        log.info("已引导创建 OAuth 客户端: device-cli (Device Authorization Grant)");
    }
}
