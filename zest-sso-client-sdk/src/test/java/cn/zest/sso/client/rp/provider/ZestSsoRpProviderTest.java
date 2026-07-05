package cn.zest.sso.client.rp.provider;

import cn.zest.sso.client.rp.RpSsoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZestSsoRpProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseLogoutUrlResponse_success() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("code", 0, "data", "http://localhost:9000/connect/logout?x=1"));

        String url = ZestSsoRpProvider.parseLogoutUrlResponse(body, objectMapper);

        assertThat(url).contains("connect/logout");
    }

    @Test
    void parseLogoutUrlResponse_nonZeroCode_throws() {
        assertThatThrownBy(() -> ZestSsoRpProvider.parseLogoutUrlResponse(
                "{\"code\":400,\"message\":\"bad\"}", objectMapper))
                .isInstanceOf(RpSsoException.class);
    }

    @Test
    void parseLogoutUrlResponse_missingData_throws() {
        assertThatThrownBy(() -> ZestSsoRpProvider.parseLogoutUrlResponse(
                "{\"code\":0}", objectMapper))
                .isInstanceOf(RpSsoException.class);
    }
}
