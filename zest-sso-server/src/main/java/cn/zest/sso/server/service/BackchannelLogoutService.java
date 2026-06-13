package cn.zest.sso.server.service;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoOAuthClient;
import cn.zest.sso.server.domain.mapper.SsoOAuthClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * OIDC Back/Front-Channel Logout 编排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackchannelLogoutService {

    private final AuthorizationAdminService authorizationAdminService;
    private final SsoOAuthClientMapper clientMapper;
    private final BackchannelLogoutNotifier backchannelLogoutNotifier;
    private final SsoProperties ssoProperties;

    public void triggerBackchannelLogout(String principalName) {
        if (!StringUtils.hasText(principalName)) {
            return;
        }
        List<SsoOAuthClient> targets = resolveLogoutTargets(principalName);
        List<SsoOAuthClient> backchannelTargets = targets.stream()
                .filter(c -> StringUtils.hasText(c.getBackchannelLogoutUri()))
                .filter(c -> isAllowedLogoutHost(c.getBackchannelLogoutUri()))
                .toList();
        if (!backchannelTargets.isEmpty()) {
            backchannelLogoutNotifier.dispatch(backchannelTargets, principalName);
        }
    }

    public List<String> resolveFrontchannelLogoutUris(String principalName) {
        List<String> uris = new ArrayList<>();
        for (SsoOAuthClient client : resolveLogoutTargets(principalName)) {
            if (StringUtils.hasText(client.getFrontchannelLogoutUri())
                    && isAllowedLogoutHost(client.getFrontchannelLogoutUri())) {
                uris.add(buildFrontchannelUrl(client.getFrontchannelLogoutUri()));
            }
        }
        return uris;
    }

    private List<SsoOAuthClient> resolveLogoutTargets(String principalName) {
        List<String> registeredClientIds = authorizationAdminService
                .findDistinctRegisteredClientIdsByPrincipal(principalName);
        List<SsoOAuthClient> targets = new ArrayList<>();
        for (String registeredClientId : registeredClientIds) {
            try {
                SsoOAuthClient client = clientMapper.selectById(Long.parseLong(registeredClientId));
                if (client != null && client.getStatus() != null && client.getStatus() == 1
                        && (StringUtils.hasText(client.getBackchannelLogoutUri())
                        || StringUtils.hasText(client.getFrontchannelLogoutUri()))) {
                    targets.add(client);
                }
            } catch (NumberFormatException ex) {
                log.debug("跳过无效 registered_client_id: {}", registeredClientId);
            }
        }
        return targets;
    }

    private String buildFrontchannelUrl(String baseUri) {
        String issuer = ssoProperties.getIssuer().replaceAll("/$", "");
        String separator = baseUri.contains("?") ? "&" : "?";
        return baseUri + separator + "iss=" + issuer;
    }

    private boolean isAllowedLogoutHost(String logoutUri) {
        try {
            URI uri = URI.create(logoutUri);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            for (String allowed : ssoProperties.getSecurity().getLogoutRedirectHosts()) {
                if (host.equals(allowed) || host.endsWith("." + allowed)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
