package cn.zest.sso.server.service;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.domain.entity.SsoWebhookDelivery;
import cn.zest.sso.server.domain.mapper.SsoWebhookDeliveryMapper;
import cn.zest.sso.server.support.RequiresMysql;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
class WebhookDeliveryServiceTest {

    @Autowired
    private WebhookDeliveryService webhookDeliveryService;

    @Autowired
    private SsoWebhookDeliveryMapper deliveryMapper;

    @Autowired
    private SsoProperties ssoProperties;

    @Test
    void shouldMarkDeliveryDeadAfterMaxRetries() {
        SsoWebhookDelivery delivery = new SsoWebhookDelivery();
        delivery.setEventType("LOGIN_SUCCESS");
        delivery.setEndpointUrl("http://127.0.0.1:1/unreachable");
        delivery.setPayloadJson("{\"event\":\"LOGIN_SUCCESS\"}");
        delivery.setStatus(SsoWebhookDelivery.STATUS_PENDING);
        delivery.setAttemptCount(ssoProperties.getWebhooks().getMaxRetries());
        deliveryMapper.insert(delivery);

        webhookDeliveryService.deliver(delivery);

        SsoWebhookDelivery updated = deliveryMapper.selectOne(new LambdaQueryWrapper<SsoWebhookDelivery>()
                .eq(SsoWebhookDelivery::getId, delivery.getId()));
        assertThat(updated.getStatus()).isEqualTo(SsoWebhookDelivery.STATUS_DEAD);
    }
}
