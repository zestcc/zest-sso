package cn.zest.sso.server.service;

import cn.zest.sso.common.constant.SsoConstants;
import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.enums.UserStatus;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.entity.SsoWebauthnCredential;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import cn.zest.sso.server.domain.mapper.SsoWebauthnCredentialMapper;
import cn.zest.sso.server.domain.vo.WebauthnCredentialVO;
import cn.zest.sso.server.domain.vo.WebauthnOptionsVO;
import cn.zest.sso.server.support.AdminAuditSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.Base64UrlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebAuthnService {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    private final SsoProperties ssoProperties;
    private final SsoUserMapper userMapper;
    private final SsoWebauthnCredentialMapper credentialMapper;
    private final StringRedisTemplate redisTemplate;
    private final WebAuthnManager webAuthnManager;
    private final ObjectConverter objectConverter;
    private final AdminAuditSupport auditSupport;

    public boolean isEnabled() {
        return ssoProperties.getWebauthn().isEnabled();
    }

    public List<WebauthnCredentialVO> listCredentials(Long userId) {
        return credentialMapper.selectList(new LambdaQueryWrapper<SsoWebauthnCredential>()
                        .eq(SsoWebauthnCredential::getUserId, userId)
                        .orderByDesc(SsoWebauthnCredential::getCreateTime))
                .stream()
                .map(c -> WebauthnCredentialVO.builder()
                        .id(c.getId())
                        .nickname(c.getNickname())
                        .createTime(c.getCreateTime())
                        .lastUsedAt(c.getLastUsedAt())
                        .build())
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCredential(Long userId, Long credentialDbId) {
        SsoWebauthnCredential credential = credentialMapper.selectById(credentialDbId);
        if (credential == null || !userId.equals(credential.getUserId())) {
            throw new SsoException(ErrorCode.NOT_FOUND, "Passkey 不存在");
        }
        credentialMapper.deleteById(credentialDbId);
        SsoUser user = userMapper.selectById(userId);
        auditSupport.log(AuditEventType.WEBAUTHN_DELETE, user.getUsername(), credential.getNickname());
    }

    public WebauthnOptionsVO beginRegistration(Long userId, String nickname, String origin) {
        assertEnabled();
        assertOrigin(origin);
        SsoUser user = requireActiveUser(userId);
        byte[] challengeBytes = randomChallenge();
        String sessionToken = storeChallenge("reg:" + userId, challengeBytes);

        PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions(
                rpEntity(),
                userEntity(user),
                new DefaultChallenge(challengeBytes),
                pubKeyCredParams(),
                null,
                null,
                authenticatorSelection(),
                AttestationConveyancePreference.NONE,
                null
        );

        return WebauthnOptionsVO.builder()
                .sessionToken(sessionToken)
                .publicKey(toPublicKeyMap(options))
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void finishRegistration(Long userId, String sessionToken, String nickname,
                                   Map<String, Object> credential, String origin) {
        assertEnabled();
        assertOrigin(origin);
        SsoUser user = requireActiveUser(userId);
        byte[] challengeBytes = consumeChallenge(sessionToken, "reg:" + userId);

        Map<String, Object> response = responseMap(credential);
        byte[] attestationObject = Base64UrlUtil.decode((String) response.get("attestationObject"));
        byte[] clientDataJSON = Base64UrlUtil.decode((String) response.get("clientDataJSON"));

        ServerProperty serverProperty = serverProperty(origin, challengeBytes);
        var registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON);
        var registrationParameters = new RegistrationParameters(serverProperty, null, false, true);
        var registrationData = webAuthnManager.validate(registrationRequest, registrationParameters);

        CredentialRecord credentialRecord = new CredentialRecordImpl(
                registrationData.getAttestationObject(),
                registrationData.getCollectedClientData(),
                registrationData.getClientExtensions(),
                registrationData.getTransports()
        );
        if (credentialRecord.getAttestedCredentialData() == null) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "无效的 WebAuthn 注册响应");
        }

        String credentialId = Base64UrlUtil.encodeToString(
                credentialRecord.getAttestedCredentialData().getCredentialId());
        if (credentialMapper.selectCount(new LambdaQueryWrapper<SsoWebauthnCredential>()
                .eq(SsoWebauthnCredential::getCredentialId, credentialId)) > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "Passkey 已注册");
        }

        SsoWebauthnCredential entity = new SsoWebauthnCredential();
        entity.setUserId(userId);
        entity.setCredentialId(credentialId);
        entity.setCredentialRecordJson(serializeCredentialRecord(credentialRecord));
        entity.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : "Passkey");
        entity.setCreateTime(LocalDateTime.now());
        credentialMapper.insert(entity);
        auditSupport.log(AuditEventType.WEBAUTHN_REGISTER, user.getUsername(), entity.getNickname());
    }

    public WebauthnOptionsVO beginAuthentication(String username, String origin) {
        assertEnabled();
        assertOrigin(origin);
        byte[] challengeBytes = randomChallenge();
        String sessionToken = storeChallenge("auth", challengeBytes);

        List<PublicKeyCredentialDescriptor> allowCredentials = new ArrayList<>();
        Boolean credentialAvailable = null;
        if (StringUtils.hasText(username)) {
            SsoUser user = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>()
                    .eq(SsoUser::getUsername, username.trim()));
            if (user != null) {
                allowCredentials.addAll(buildAllowCredentials(user.getId()));
                credentialAvailable = !allowCredentials.isEmpty();
            } else {
                credentialAvailable = false;
            }
        }

        PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions(
                new DefaultChallenge(challengeBytes),
                300_000L,
                ssoProperties.getWebauthn().getRpId(),
                allowCredentials.isEmpty() ? null : allowCredentials,
                UserVerificationRequirement.PREFERRED,
                null
        );

        return WebauthnOptionsVO.builder()
                .sessionToken(sessionToken)
                .publicKey(toPublicKeyMap(options))
                .credentialAvailable(credentialAvailable)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long finishAuthentication(String sessionToken, Map<String, Object> credential, String origin) {
        assertEnabled();
        assertOrigin(origin);
        byte[] challengeBytes = consumeChallenge(sessionToken, "auth");

        String credentialId = credentialIdFromClient(credential);
        SsoWebauthnCredential stored = credentialMapper.selectOne(new LambdaQueryWrapper<SsoWebauthnCredential>()
                .eq(SsoWebauthnCredential::getCredentialId, credentialId));
        if (stored == null) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "未找到 Passkey");
        }

        Map<String, Object> response = responseMap(credential);
        byte[] authenticatorData = Base64UrlUtil.decode((String) response.get("authenticatorData"));
        byte[] clientDataJSON = Base64UrlUtil.decode((String) response.get("clientDataJSON"));
        byte[] signature = Base64UrlUtil.decode((String) response.get("signature"));

        ServerProperty serverProperty = serverProperty(origin, challengeBytes);
        CredentialRecord record = deserializeCredentialRecord(stored.getCredentialRecordJson());
        var authenticationRequest = new AuthenticationRequest(
                Base64UrlUtil.decode(credentialId),
                authenticatorData,
                clientDataJSON,
                signature,
                null
        );
        var authenticationParameters = new AuthenticationParameters(serverProperty, record, null, false);
        var authenticationData = webAuthnManager.validate(authenticationRequest, authenticationParameters);

        long newCounter = authenticationData.getAuthenticatorData().getSignCount();
        long oldCounter = record.getCounter();
        if (newCounter > 0 && newCounter <= oldCounter) {
            throw new SsoException(ErrorCode.FORBIDDEN, "Passkey 签名计数异常，可能遭克隆攻击");
        }

        record.setCounter(newCounter);
        stored.setCredentialRecordJson(serializeCredentialRecord(record));
        stored.setLastUsedAt(LocalDateTime.now());
        credentialMapper.updateById(stored);

        SsoUser user = requireActiveUser(stored.getUserId());
        auditSupport.log(AuditEventType.WEBAUTHN_LOGIN, user.getUsername(), stored.getNickname());
        return user.getId();
    }

    private List<PublicKeyCredentialDescriptor> buildAllowCredentials(Long userId) {
        return credentialMapper.selectList(new LambdaQueryWrapper<SsoWebauthnCredential>()
                        .eq(SsoWebauthnCredential::getUserId, userId))
                .stream()
                .map(c -> new PublicKeyCredentialDescriptor(
                        PublicKeyCredentialType.PUBLIC_KEY,
                        Base64UrlUtil.decode(c.getCredentialId()),
                        null))
                .toList();
    }

    private PublicKeyCredentialRpEntity rpEntity() {
        SsoProperties.WebAuthn cfg = ssoProperties.getWebauthn();
        return new PublicKeyCredentialRpEntity(cfg.getRpName(), cfg.getRpId());
    }

    private PublicKeyCredentialUserEntity userEntity(SsoUser user) {
        return new PublicKeyCredentialUserEntity(
                userHandle(user.getId()),
                user.getUsername(),
                user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()
        );
    }

    private List<PublicKeyCredentialParameters> pubKeyCredParams() {
        return List.of(
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256)
        );
    }

    private AuthenticatorSelectionCriteria authenticatorSelection() {
        return new AuthenticatorSelectionCriteria(
                null,
                ResidentKeyRequirement.PREFERRED,
                UserVerificationRequirement.PREFERRED
        );
    }

    private ServerProperty serverProperty(String origin, byte[] challengeBytes) {
        Challenge challenge = new DefaultChallenge(challengeBytes);
        return new ServerProperty(Origin.create(origin), ssoProperties.getWebauthn().getRpId(), challenge, null);
    }

    private byte[] userHandle(Long userId) {
        return ByteBuffer.allocate(8).putLong(userId).array();
    }

    private String serializeCredentialRecord(CredentialRecord record) {
        try {
            return objectConverter.getJsonConverter().writeValueAsString(record);
        } catch (Exception ex) {
            throw new SsoException(ErrorCode.INTERNAL_ERROR, "凭据序列化失败");
        }
    }

    private CredentialRecord deserializeCredentialRecord(String json) {
        try {
            return objectConverter.getJsonConverter().readValue(json, CredentialRecord.class);
        } catch (Exception ex) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "凭据数据损坏");
        }
    }

    private byte[] randomChallenge() {
        byte[] challenge = new byte[32];
        new SecureRandom().nextBytes(challenge);
        return challenge;
    }

    private String storeChallenge(String scope, byte[] challengeBytes) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = SsoConstants.REDIS_WEBAUTHN_CHALLENGE_PREFIX + scope + ":" + token;
        redisTemplate.opsForValue().set(key, Base64UrlUtil.encodeToString(challengeBytes), CHALLENGE_TTL);
        return token;
    }

    private byte[] consumeChallenge(String sessionToken, String scope) {
        String key = SsoConstants.REDIS_WEBAUTHN_CHALLENGE_PREFIX + scope + ":" + sessionToken;
        String encoded = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(encoded)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "WebAuthn 挑战无效或已过期");
        }
        redisTemplate.delete(key);
        return Base64UrlUtil.decode(encoded);
    }

    private String credentialIdFromClient(Map<String, Object> credential) {
        Object rawId = credential.get("rawId");
        if (rawId == null) {
            rawId = credential.get("id");
        }
        if (rawId == null) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "缺少 credential id");
        }
        if (rawId instanceof String s) {
            return s;
        }
        throw new SsoException(ErrorCode.BAD_REQUEST, "credential id 格式无效");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> responseMap(Map<String, Object> credential) {
        Object response = credential.get("response");
        if (!(response instanceof Map<?, ?> map)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "无效的 WebAuthn 响应");
        }
        return (Map<String, Object>) map;
    }

    private Map<String, Object> toPublicKeyMap(PublicKeyCredentialRequestOptions options) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("challenge", Base64UrlUtil.encodeToString(options.getChallenge().getValue()));
        map.put("timeout", options.getTimeout());
        map.put("rpId", options.getRpId());
        if (options.getAllowCredentials() != null && !options.getAllowCredentials().isEmpty()) {
            map.put("allowCredentials", options.getAllowCredentials().stream()
                    .map(this::descriptorToMap)
                    .toList());
        }
        if (options.getUserVerification() != null) {
            map.put("userVerification", options.getUserVerification().getValue());
        }
        return map;
    }

    private Map<String, Object> toPublicKeyMap(PublicKeyCredentialCreationOptions options) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rp", Map.of(
                "name", options.getRp().getName(),
                "id", options.getRp().getId() != null ? options.getRp().getId() : ssoProperties.getWebauthn().getRpId()
        ));
        map.put("user", Map.of(
                "id", Base64UrlUtil.encodeToString(options.getUser().getId()),
                "name", options.getUser().getName(),
                "displayName", options.getUser().getDisplayName()
        ));
        map.put("challenge", Base64UrlUtil.encodeToString(options.getChallenge().getValue()));
        map.put("pubKeyCredParams", options.getPubKeyCredParams().stream()
                .map(p -> Map.of(
                        "type", p.getType().getValue(),
                        "alg", p.getAlg().getValue()
                ))
                .toList());
        if (options.getTimeout() != null) {
            map.put("timeout", options.getTimeout());
        }
        if (options.getExcludeCredentials() != null && !options.getExcludeCredentials().isEmpty()) {
            map.put("excludeCredentials", options.getExcludeCredentials().stream()
                    .map(this::descriptorToMap)
                    .toList());
        }
        if (options.getAuthenticatorSelection() != null) {
            Map<String, Object> selection = new LinkedHashMap<>();
            if (options.getAuthenticatorSelection().getResidentKey() != null) {
                selection.put("residentKey", options.getAuthenticatorSelection().getResidentKey().getValue());
            }
            if (options.getAuthenticatorSelection().getUserVerification() != null) {
                selection.put("userVerification", options.getAuthenticatorSelection().getUserVerification().getValue());
            }
            if (!selection.isEmpty()) {
                map.put("authenticatorSelection", selection);
            }
        }
        if (options.getAttestation() != null) {
            map.put("attestation", options.getAttestation().getValue());
        }
        return map;
    }

    private Map<String, Object> descriptorToMap(PublicKeyCredentialDescriptor descriptor) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", descriptor.getType().getValue());
        item.put("id", Base64UrlUtil.encodeToString(descriptor.getId()));
        if (descriptor.getTransports() != null && !descriptor.getTransports().isEmpty()) {
            item.put("transports", descriptor.getTransports().stream()
                    .map(AuthenticatorTransport::getValue)
                    .toList());
        }
        return item;
    }

    private SsoUser requireActiveUser(Long userId) {
        SsoUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != UserStatus.ACTIVE.getCode()) {
            throw new SsoException(ErrorCode.FORBIDDEN, "用户已禁用");
        }
        return user;
    }

    public String resolveWebOrigin(String originHeader, String refererHeader) {
        if (StringUtils.hasText(originHeader)) {
            return originHeader;
        }
        if (!StringUtils.hasText(refererHeader)) {
            return null;
        }
        try {
            URI uri = URI.create(refererHeader);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }
            int port = uri.getPort();
            if (port > 0) {
                return scheme + "://" + host + ":" + port;
            }
            return scheme + "://" + host;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void assertEnabled() {
        if (!isEnabled()) {
            throw new SsoException(ErrorCode.FORBIDDEN, "WebAuthn 未启用");
        }
    }

    private void assertOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "缺少 Origin");
        }
        if (!ssoProperties.getWebauthn().getOrigins().contains(origin)) {
            throw new SsoException(ErrorCode.FORBIDDEN, "Origin 不在白名单: " + origin);
        }
    }
}
