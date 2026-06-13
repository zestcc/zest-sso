package cn.zest.sso.server.service;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoJwtSigningKey;
import cn.zest.sso.server.domain.mapper.SsoJwtSigningKeyMapper;
import cn.zest.sso.server.domain.vo.JwtSigningKeyVO;
import cn.zest.sso.server.security.JwtKeyManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * JWT 双 key 轮换（对标 Okta/Auth0 key rollover）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtSigningKeyService {

    private final SsoJwtSigningKeyMapper keyMapper;
    private final SsoProperties ssoProperties;
    private final JwtKeyManager jwtKeyManager;

    @Getter
    private volatile SsoJwtSigningKey activeKey;

    @PostConstruct
    public void bootstrap() throws Exception {
        SsoJwtSigningKey existing = keyMapper.selectOne(new LambdaQueryWrapper<SsoJwtSigningKey>()
                .eq(SsoJwtSigningKey::getStatus, SsoJwtSigningKey.STATUS_ACTIVE)
                .orderByDesc(SsoJwtSigningKey::getCreateTime)
                .last("LIMIT 1"));
        if (existing != null) {
            activeKey = existing;
            syncJwtKeyManager(existing);
            log.info("JWT 签名密钥已从数据库加载: kid={}", existing.getKeyId());
            return;
        }
        importFromFileManager();
    }

    @Transactional(rollbackFor = Exception.class)
    public SsoJwtSigningKey rotate() throws Exception {
        if (activeKey != null) {
            SsoJwtSigningKey retire = new SsoJwtSigningKey();
            retire.setId(activeKey.getId());
            retire.setStatus(SsoJwtSigningKey.STATUS_INACTIVE);
            retire.setNotAfter(LocalDateTime.now().plusDays(7));
            keyMapper.updateById(retire);
        }
        SsoJwtSigningKey created = createAndPersistKey();
        activeKey = created;
        syncJwtKeyManager(created);
        purgeExpiredInactiveKeys();
        log.info("JWT 密钥已轮换: newKid={}", created.getKeyId());
        return created;
    }

    public JWKSet buildJwkSet() throws Exception {
        List<JWK> keys = new ArrayList<>();
        if (activeKey != null) {
            keys.add(toRsaKey(activeKey, true));
        }
        int retiredLimit = ssoProperties.getJwtRotation().getRetiredKeysInJwks();
        if (retiredLimit > 0) {
            List<SsoJwtSigningKey> retired = keyMapper.selectList(new LambdaQueryWrapper<SsoJwtSigningKey>()
                    .eq(SsoJwtSigningKey::getStatus, SsoJwtSigningKey.STATUS_INACTIVE)
                    .orderByDesc(SsoJwtSigningKey::getCreateTime)
                    .last("LIMIT " + retiredLimit));
            for (SsoJwtSigningKey key : retired) {
                keys.add(toRsaKey(key, false));
            }
        }
        if (keys.isEmpty()) {
            RSAKey fallback = new RSAKey.Builder(jwtKeyManager.getPublicKey())
                    .privateKey(jwtKeyManager.getPrivateKey())
                    .keyID(jwtKeyManager.getKeyId())
                    .build();
            return new JWKSet(fallback);
        }
        return new JWKSet(keys);
    }

    /** 签发 JWT 时仅使用当前 ACTIVE 私钥（避免 Nimbus 多 key 歧义）。 */
    public RSAKey activeSigningKey() throws Exception {
        if (activeKey != null) {
            return toRsaKey(activeKey, true);
        }
        return new RSAKey.Builder(jwtKeyManager.getPublicKey())
                .privateKey(jwtKeyManager.getPrivateKey())
                .keyID(jwtKeyManager.getKeyId())
                .build();
    }

    public String activeKeyId() {
        return activeKey != null ? activeKey.getKeyId() : jwtKeyManager.getKeyId();
    }

    public List<JwtSigningKeyVO> listKeys() {
        return keyMapper.selectList(new LambdaQueryWrapper<SsoJwtSigningKey>()
                        .orderByDesc(SsoJwtSigningKey::getCreateTime))
                .stream()
                .map(key -> JwtSigningKeyVO.builder()
                        .id(key.getId())
                        .keyId(key.getKeyId())
                        .status(key.getStatus())
                        .notAfter(key.getNotAfter())
                        .createTime(key.getCreateTime())
                        .build())
                .toList();
    }

    private void importFromFileManager() throws Exception {
        SsoJwtSigningKey created = new SsoJwtSigningKey();
        created.setKeyId(ssoProperties.getJwt().getKeyId());
        created.setPublicPem(toPublicPem(jwtKeyManager.getPublicKey()));
        created.setPrivatePem(toPrivatePem(jwtKeyManager.getPrivateKey()));
        created.setStatus(SsoJwtSigningKey.STATUS_ACTIVE);
        keyMapper.insert(created);
        activeKey = created;
        log.info("JWT 密钥已从文件导入数据库: kid={}", created.getKeyId());
    }

    private SsoJwtSigningKey createAndPersistKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        SsoJwtSigningKey created = new SsoJwtSigningKey();
        created.setKeyId("zest-sso-key-" + UUID.randomUUID().toString().substring(0, 8));
        created.setPublicPem(toPublicPem(publicKey));
        created.setPrivatePem(toPrivatePem(privateKey));
        created.setStatus(SsoJwtSigningKey.STATUS_ACTIVE);
        keyMapper.insert(created);
        return created;
    }

    private void syncJwtKeyManager(SsoJwtSigningKey key) throws Exception {
        jwtKeyManager.loadFromPem(key.getPrivatePem(), key.getPublicPem(), key.getKeyId());
    }

    private void purgeExpiredInactiveKeys() {
        keyMapper.delete(new LambdaQueryWrapper<SsoJwtSigningKey>()
                .eq(SsoJwtSigningKey::getStatus, SsoJwtSigningKey.STATUS_INACTIVE)
                .lt(SsoJwtSigningKey::getNotAfter, LocalDateTime.now()));
    }

    private RSAKey toRsaKey(SsoJwtSigningKey key, boolean includePrivate) throws Exception {
        RSAPublicKey publicKey = parsePublicKey(key.getPublicPem());
        RSAKey.Builder builder = new RSAKey.Builder(publicKey).keyID(key.getKeyId());
        if (includePrivate) {
            builder.privateKey(parsePrivateKey(key.getPrivatePem()));
        }
        return builder.build();
    }

    private static RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String content = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private static RSAPublicKey parsePublicKey(String pem) throws Exception {
        String content = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private static String toPrivatePem(RSAPrivateKey key) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n";
    }

    private static String toPublicPem(RSAPublicKey key) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n";
    }
}
