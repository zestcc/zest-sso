package cn.zest.sso.plugin.tencent.sms;

import cn.zest.sso.plugin.PluginRuntimeContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(PluginRuntimeContext.class)
public class TencentSmsPluginAutoConfiguration {

    @Bean
    public TencentSmsMfaChannelAdapter tencentSmsMfaChannelAdapter(PluginRuntimeContext pluginRuntimeContext,
                                                                   StringRedisTemplate redisTemplate) {
        return new TencentSmsMfaChannelAdapter(pluginRuntimeContext, redisTemplate);
    }
}
