package cn.zest.sso.server.job;

import cn.zest.sso.server.service.BackchannelLogoutDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackchannelLogoutRetryJob {

    private final BackchannelLogoutDeliveryService deliveryService;

    @Scheduled(fixedDelay = 60_000)
    public void retryFailedDeliveries() {
        int count = deliveryService.retryDueDeliveries();
        if (count > 0) {
            log.debug("Back-Channel 重试任务处理 {} 条投递", count);
        }
    }
}
