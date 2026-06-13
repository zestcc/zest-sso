package cn.zest.sso.server;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.alert.AlertNotificationService;
import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.domain.entity.SsoWebhookDelivery;
import cn.zest.sso.server.domain.mapper.SsoWebhookDeliveryMapper;
import cn.zest.sso.server.domain.vo.AlertChannelConfigVO;
import cn.zest.sso.server.plugin.AlertChannelConfigService;
import cn.zest.sso.server.support.RequiresMysql;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 告警 DB 在线配置优先于 YAML，触发 Webhook 入队。
 */
@SpringBootTest
@ActiveProfiles("test")
@RequiresMysql
@Import(TestRedisConfig.class)
class AlertNotificationServiceIT {

    @Autowired
    private AlertNotificationService alertNotificationService;

    @Autowired
    private AlertChannelConfigService alertChannelConfigService;

    @Autowired
    private SsoWebhookDeliveryMapper deliveryMapper;

    @DynamicPropertySource
    static void enableAlerts(DynamicPropertyRegistry registry) {
        registry.add("zest.sso.modules.alerts", () -> "true");
    }

    @Test
    void shouldPublishViaDatabaseAlertChannel() {
        String endpoint = "http://127.0.0.1:19998/db-alert-" + System.currentTimeMillis();
        AlertChannelConfigVO created = alertChannelConfigService.create(AlertChannelConfigVO.builder()
                .name("IT DB Alert")
                .channelKey("http-webhook")
                .enabled(1)
                .events(List.of(AuditEventType.LOGIN_SUCCESS.getCode()))
                .config(java.util.Map.of("url", endpoint))
                .build());

        alertNotificationService.publish(AuditEventType.LOGIN_SUCCESS, "admin", "admin", "acceptance");

        List<SsoWebhookDelivery> deliveries = deliveryMapper.selectList(new LambdaQueryWrapper<SsoWebhookDelivery>()
                .eq(SsoWebhookDelivery::getEndpointUrl, endpoint));
        assertThat(deliveries).isNotEmpty();
        assertThat(deliveries.get(0).getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS.getCode());
        assertThat(deliveries.get(0).getPayloadJson()).contains("acceptance");

        alertChannelConfigService.delete(created.getId());
    }

    @Test
    void shouldSkipEventNotInSubscription() {
        String endpoint = "http://127.0.0.1:19997/skip-" + System.currentTimeMillis();
        AlertChannelConfigVO created = alertChannelConfigService.create(AlertChannelConfigVO.builder()
                .name("IT Skip Event")
                .channelKey("http-webhook")
                .enabled(1)
                .events(List.of("USER_DELETE"))
                .config(java.util.Map.of("url", endpoint))
                .build());

        alertNotificationService.publish(AuditEventType.LOGIN_SUCCESS, "admin", "admin", "should-skip");

        long count = deliveryMapper.selectCount(new LambdaQueryWrapper<SsoWebhookDelivery>()
                .eq(SsoWebhookDelivery::getEndpointUrl, endpoint));
        assertThat(count).isZero();

        alertChannelConfigService.delete(created.getId());
    }
}
