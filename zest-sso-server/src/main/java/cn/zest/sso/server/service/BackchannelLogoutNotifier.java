package cn.zest.sso.server.service;

import cn.zest.sso.server.domain.entity.SsoOAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BackchannelLogoutNotifier {

    private final BackchannelLogoutDeliveryService deliveryService;

    @Async("taskExecutor")
    public void dispatch(List<SsoOAuthClient> targets, String principalName) {
        deliveryService.enqueueDeliveries(targets, principalName);
    }
}
