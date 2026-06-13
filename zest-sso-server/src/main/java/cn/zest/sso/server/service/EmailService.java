package cn.zest.sso.server.service;

import cn.zest.sso.server.config.SsoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 邮件通知；未配置 SMTP 时仅记录日志（开发环境友好）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SsoProperties ssoProperties;
    private final JavaMailSender mailSender;

    public void sendPasswordReset(String toEmail, String resetUrl, int ttlMinutes) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("用户无邮箱，跳过邮件发送，重置链接: {}", resetUrl);
            return;
        }
        if (!ssoProperties.getMail().isEnabled()) {
            log.info("[邮件未启用] 发送至 {} 的密码重置链接: {} ({}分钟内有效)", toEmail, resetUrl, ttlMinutes);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(ssoProperties.getMail().getFrom());
        message.setTo(toEmail);
        message.setSubject(ssoProperties.getMail().getResetSubject());
        message.setText("""
                您好，

                您申请了 ZestSSO 密码重置。请在 %d 分钟内访问以下链接设置新密码：

                %s

                如非本人操作，请忽略此邮件。
                """.formatted(ttlMinutes, resetUrl));
        mailSender.send(message);
        log.info("密码重置邮件已发送至 {}", toEmail);
    }

    public void sendLoginOtp(String toEmail, String code, int ttlMinutes) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("用户无邮箱，跳过 OTP 邮件，验证码: {}", code);
            return;
        }
        if (!ssoProperties.getMail().isEnabled()) {
            log.info("[邮件未启用] 发送至 {} 的登录验证码: {} ({}分钟内有效)", toEmail, code, ttlMinutes);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(ssoProperties.getMail().getFrom());
        message.setTo(toEmail);
        message.setSubject("ZestSSO 登录验证码");
        message.setText("""
                您好，

                检测到新设备或异地登录，您的验证码为：%s

                请在 %d 分钟内完成验证。如非本人操作，请立即联系管理员。
                """.formatted(code, ttlMinutes));
        mailSender.send(message);
        log.info("登录 OTP 邮件已发送至 {}", toEmail);
    }
}
