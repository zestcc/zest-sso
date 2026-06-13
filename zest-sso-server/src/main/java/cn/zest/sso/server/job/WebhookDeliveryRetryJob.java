package cn.zest.sso.server.job;

import cn.zest.sso.server.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebhookDeliveryRetryJob {

    private final WebhookDeliveryService webhookDeliveryService;

    @Scheduled(fixedDelayString = "${zest.sso.webhooks.retry-scan-ms:30000}")
    public void retryDueDeliveries() {
        webhookDeliveryService.retryDueDeliveries();
    }
}
