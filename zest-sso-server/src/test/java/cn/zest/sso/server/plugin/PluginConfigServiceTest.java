package cn.zest.sso.server.plugin;

import cn.zest.sso.server.domain.entity.SsoPluginConfig;
import cn.zest.sso.server.domain.mapper.SsoPluginConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginConfigServiceTest {

    @Mock
    private SsoPluginConfigMapper pluginConfigMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PluginConfigService pluginConfigService;

    @Test
    void shouldMaskSecretFields() {
        Map<String, String> masked = pluginConfigService.maskSecrets(Map.of(
                "accessKeyId", "ak",
                "accessKeySecret", "sk",
                "signName", "Zest"));
        assertThat(masked.get("accessKeyId")).isEqualTo("ak");
        assertThat(masked.get("accessKeySecret")).isEqualTo("******");
        assertThat(masked.get("signName")).isEqualTo("Zest");
    }

    @Test
    void shouldInsertPluginConfigWhenNotExists() {
        when(pluginConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        pluginConfigService.save("aliyun-sms", true, Map.of("accessKeyId", "ak"));

        ArgumentCaptor<SsoPluginConfig> captor = ArgumentCaptor.forClass(SsoPluginConfig.class);
        verify(pluginConfigMapper).insert(captor.capture());
        assertThat(captor.getValue().getPluginKey()).isEqualTo("aliyun-sms");
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
        assertThat(captor.getValue().getConfig()).contains("ak");
    }
}
