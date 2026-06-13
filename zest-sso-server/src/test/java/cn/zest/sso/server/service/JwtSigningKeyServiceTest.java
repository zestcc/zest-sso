package cn.zest.sso.server.service;

import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.support.RequiresMysql;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@RequiresMysql
@Import(TestRedisConfig.class)
class JwtSigningKeyServiceTest {

    @Autowired
    private JwtSigningKeyService jwtSigningKeyService;

    @Test
    void shouldRotateJwtSigningKey() throws Exception {
        String before = jwtSigningKeyService.activeKeyId();
        var rotated = jwtSigningKeyService.rotate();
        assertThat(rotated.getKeyId()).isNotEqualTo(before);
        assertThat(jwtSigningKeyService.listKeys()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(jwtSigningKeyService.buildJwkSet().getKeys()).hasSizeGreaterThanOrEqualTo(2);
    }
}
