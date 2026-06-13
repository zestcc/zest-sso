package cn.zest.sso.server.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 创建/重置密钥后的客户端响应，仅此时返回明文 clientSecret。
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CreateClientResultVO extends ClientVO {

    private String clientSecret;
    private ClientOnboardingVO onboarding;
}
