package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.dto.CreateClientRequest;
import cn.zest.sso.server.domain.dto.UpdateClientRequest;
import cn.zest.sso.server.domain.entity.SsoOAuthClient;
import cn.zest.sso.server.domain.mapper.SsoOAuthClientMapper;
import cn.zest.sso.server.domain.vo.ClientVO;
import cn.zest.sso.server.domain.vo.CreateClientResultVO;
import cn.zest.sso.server.support.AdminAuditSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OAuthClientService {

    private final SsoOAuthClientMapper clientMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditSupport auditSupport;

    public Page<ClientVO> pageClients(int page, int size) {
        Page<SsoOAuthClient> clientPage = clientMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SsoOAuthClient>().orderByDesc(SsoOAuthClient::getCreateTime));
        Page<ClientVO> result = new Page<>(page, size, clientPage.getTotal());
        result.setRecords(clientPage.getRecords().stream().map(this::toClientVO).toList());
        return result;
    }

    public ClientVO getByClientId(String clientId) {
        return toClientVO(findClient(clientId));
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateClientResultVO createClient(CreateClientRequest request) {
        Long count = clientMapper.selectCount(new LambdaQueryWrapper<SsoOAuthClient>()
                .eq(SsoOAuthClient::getClientId, request.getClientId()));
        if (count > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "clientId 已存在");
        }

        SsoOAuthClient client = new SsoOAuthClient();
        client.setClientId(request.getClientId());
        client.setClientSecretHash(passwordEncoder.encode(request.getClientSecret()));
        client.setClientName(request.getClientName());
        client.setClientAuthenticationMethods("client_secret_basic,client_secret_post,none");
        client.setAuthorizationGrantTypes(joinOrDefault(request.getAuthorizationGrantTypes(),
                "authorization_code,refresh_token"));
        client.setRedirectUris(joinOrDefault(request.getRedirectUris(), ""));
        client.setScopes(joinOrDefault(request.getScopes(), "openid,profile,email,roles,tenant"));
        client.setRequirePkce(request.getRequirePkce() != null && request.getRequirePkce() ? 1 : 0);
        client.setRequireConsent(request.getRequireConsent() != null && request.getRequireConsent() ? 1 : 0);
        client.setAccessTokenTtl(request.getAccessTokenTtl() != null ? request.getAccessTokenTtl() : 3600);
        client.setRefreshTokenTtl(request.getRefreshTokenTtl() != null ? request.getRefreshTokenTtl() : 86400);
        client.setBackchannelLogoutUri(request.getBackchannelLogoutUri());
        client.setFrontchannelLogoutUri(request.getFrontchannelLogoutUri());
        client.setStatus(1);
        clientMapper.insert(client);
        auditSupport.log(AuditEventType.CLIENT_CREATE, client.getClientId(), client.getClientName());
        return toCreateClientResultVO(client, request.getClientSecret());
    }

    @Transactional(rollbackFor = Exception.class)
    public ClientVO updateClient(String clientId, UpdateClientRequest request) {
        SsoOAuthClient client = findClient(clientId);
        if (request.getClientName() != null) {
            client.setClientName(request.getClientName());
        }
        if (request.getAuthorizationGrantTypes() != null) {
            client.setAuthorizationGrantTypes(joinOrDefault(request.getAuthorizationGrantTypes(), ""));
        }
        if (request.getRedirectUris() != null) {
            client.setRedirectUris(joinOrDefault(request.getRedirectUris(), ""));
        }
        if (request.getScopes() != null) {
            client.setScopes(joinOrDefault(request.getScopes(), ""));
        }
        if (request.getRequirePkce() != null) {
            client.setRequirePkce(request.getRequirePkce() ? 1 : 0);
        }
        if (request.getRequireConsent() != null) {
            client.setRequireConsent(request.getRequireConsent() ? 1 : 0);
        }
        if (request.getAccessTokenTtl() != null) {
            client.setAccessTokenTtl(request.getAccessTokenTtl());
        }
        if (request.getRefreshTokenTtl() != null) {
            client.setRefreshTokenTtl(request.getRefreshTokenTtl());
        }
        if (request.getBackchannelLogoutUri() != null) {
            client.setBackchannelLogoutUri(blankToNull(request.getBackchannelLogoutUri()));
        }
        if (request.getFrontchannelLogoutUri() != null) {
            client.setFrontchannelLogoutUri(blankToNull(request.getFrontchannelLogoutUri()));
        }
        clientMapper.updateById(client);
        auditSupport.log(AuditEventType.CLIENT_UPDATE, client.getClientId(), client.getClientName());
        return toClientVO(client);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enableClient(String clientId) {
        updateStatus(clientId, 1, AuditEventType.CLIENT_ENABLE);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableClient(String clientId) {
        updateStatus(clientId, 0, AuditEventType.CLIENT_DISABLE);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteClient(String clientId) {
        SsoOAuthClient client = findClient(clientId);
        clientMapper.deleteById(client.getId());
        auditSupport.log(AuditEventType.CLIENT_DELETE, client.getClientId(), client.getClientName());
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateClientResultVO resetClientSecret(String clientId) {
        SsoOAuthClient client = findClient(clientId);
        String newSecret = generateSecret();
        client.setClientSecretHash(passwordEncoder.encode(newSecret));
        clientMapper.updateById(client);
        auditSupport.log(AuditEventType.CLIENT_RESET_SECRET, client.getClientId(), client.getClientName());
        return toCreateClientResultVO(client, newSecret);
    }

    private void updateStatus(String clientId, int status, AuditEventType eventType) {
        SsoOAuthClient client = findClient(clientId);
        client.setStatus(status);
        clientMapper.updateById(client);
        auditSupport.log(eventType, client.getClientId(), client.getClientName());
    }

    private SsoOAuthClient findClient(String clientId) {
        SsoOAuthClient client = clientMapper.selectOne(new LambdaQueryWrapper<SsoOAuthClient>()
                .eq(SsoOAuthClient::getClientId, clientId));
        if (client == null) {
            throw new SsoException(ErrorCode.CLIENT_NOT_FOUND, "客户端不存在");
        }
        return client;
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String joinOrDefault(List<String> values, String defaultValue) {
        if (CollectionUtils.isEmpty(values)) {
            return defaultValue;
        }
        return String.join(",", values);
    }

    private CreateClientResultVO toCreateClientResultVO(SsoOAuthClient client, String clientSecret) {
        return CreateClientResultVO.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .authorizationGrantTypes(splitToList(client.getAuthorizationGrantTypes()))
                .redirectUris(splitToList(client.getRedirectUris()))
                .scopes(splitToList(client.getScopes()))
                .requirePkce(client.getRequirePkce() != null && client.getRequirePkce() == 1)
                .requireConsent(client.getRequireConsent() != null && client.getRequireConsent() == 1)
                .accessTokenTtl(client.getAccessTokenTtl())
                .refreshTokenTtl(client.getRefreshTokenTtl())
                .backchannelLogoutUri(client.getBackchannelLogoutUri())
                .frontchannelLogoutUri(client.getFrontchannelLogoutUri())
                .status(client.getStatus())
                .clientSecret(clientSecret)
                .build();
    }

    private ClientVO toClientVO(SsoOAuthClient client) {
        return ClientVO.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .authorizationGrantTypes(splitToList(client.getAuthorizationGrantTypes()))
                .redirectUris(splitToList(client.getRedirectUris()))
                .scopes(splitToList(client.getScopes()))
                .requirePkce(client.getRequirePkce() != null && client.getRequirePkce() == 1)
                .requireConsent(client.getRequireConsent() != null && client.getRequireConsent() == 1)
                .accessTokenTtl(client.getAccessTokenTtl())
                .refreshTokenTtl(client.getRefreshTokenTtl())
                .backchannelLogoutUri(client.getBackchannelLogoutUri())
                .frontchannelLogoutUri(client.getFrontchannelLogoutUri())
                .status(client.getStatus())
                .build();
    }

    private String blankToNull(String value) {
        return value != null && value.isBlank() ? null : value;
    }

    private List<String> splitToList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
