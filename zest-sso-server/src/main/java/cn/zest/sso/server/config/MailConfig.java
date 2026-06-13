package cn.zest.sso.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 邮件配置；默认禁用，生产通过 spring.mail.* + zest.sso.mail.enabled=true 启用。
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "zest.sso.mail.enabled", havingValue = "false", matchIfMissing = true)
    public JavaMailSender noopMailSender() {
        return new JavaMailSenderImpl();
    }
}
