package cn.zest.sso.server.service;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoBackchannelLogoutDelivery;
import cn.zest.sso.server.domain.entity.SsoOAuthClient;
import cn.zest.sso.server.domain.mapper.SsoBackchannelLogoutDeliveryMapper;
import cn.zest.sso.server.metrics.SsoMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackchannelLogoutDeliveryServiceTest {

    @Mock
    private SsoBackchannelLogoutDeliveryMapper deliveryMapper;
    @Mock
    private LogoutTokenService logoutTokenService;
    @Mock
    private SsoProperties ssoProperties;
    @Mock
    private SsoMetrics ssoMetrics;
    @Mock
    private RestTemplate logoutRestTemplate;

    private BackchannelLogoutDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        deliveryService = new BackchannelLogoutDeliveryService(
                deliveryMapper, logoutTokenService, ssoProperties, ssoMetrics, logoutRestTemplate);
        SsoProperties.Backchannel backchannel = new SsoProperties.Backchannel();
        backchannel.setMaxRetries(2);
        backchannel.setRetryDelaySeconds(30);
        lenient().when(ssoProperties.getBackchannel()).thenReturn(backchannel);
    }

    @Test
    void shouldMarkSuccessWhenRpReturns2xx() {
        when(logoutTokenService.createLogoutToken("admin", "zestflow-admin")).thenReturn("logout-jwt");
        when(logoutRestTemplate.postForEntity(any(String.class), any(), any(Class.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        SsoOAuthClient client = new SsoOAuthClient();
        client.setClientId("zestflow-admin");
        client.setBackchannelLogoutUri("http://localhost:8080/backchannel");

        deliveryService.enqueueDeliveries(List.of(client), "admin");

        verify(deliveryMapper).insert(any(SsoBackchannelLogoutDelivery.class));
        ArgumentCaptor<SsoBackchannelLogoutDelivery> updateCaptor =
                ArgumentCaptor.forClass(SsoBackchannelLogoutDelivery.class);
        verify(deliveryMapper).updateById(updateCaptor.capture());
        assertEquals(SsoBackchannelLogoutDelivery.STATUS_SUCCESS, updateCaptor.getValue().getStatus());
    }
}
