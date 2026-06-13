package cn.zest.sso.server.federation.spi;

import cn.zest.sso.server.domain.dto.CreateIdentityProviderRequest;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * 联邦身份源插拔适配器 — 将 DB 中的身份源配置转换为 Spring Security 可消费的注册信息。
 * <p>
 * 新增平台（如钉钉、企微）只需实现本接口并注册为 Spring Bean，无需修改核心仓库代码。
 */
public interface FederatedIdpAdapter {

    /** 唯一键，与 {@link SsoIdentityProvider#getAdapterKey()} 对应，如 {@code feishu}、{@code dingtalk} */
    String adapterKey();

    /** Admin 控制台展示用元数据 */
    FederatedIdpAdapterDescriptor descriptor();

    boolean supports(SsoIdentityProvider provider);

    ClientRegistration buildClientRegistration(SsoIdentityProvider provider);

    /** 创建前校验；抛 {@link cn.zest.sso.common.exception.SsoException} 表示不合法 */
    default void validateCreate(CreateIdentityProviderRequest request) {
        validateCreate(request, null);
    }

    /**
     * 创建前校验（含 applyDefaults 后的预览实体），用于识别适配器内置 Discovery/端点。
     */
    default void validateCreate(CreateIdentityProviderRequest request, SsoIdentityProvider preview) {
    }

    /** 写入 DB 前填充平台默认 scopes / claims / discovery */
    default void applyDefaults(SsoIdentityProvider provider) {
    }
}
