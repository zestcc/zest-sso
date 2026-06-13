package cn.zest.sso.server.federation.spi;

import java.util.Map;

/**
 * 适配器元数据，供 Admin 选择与文档生成。
 */
public record FederatedIdpAdapterDescriptor(
        String key,
        String displayName,
        String description,
        boolean discoverySupported,
        boolean manualEndpointsSupported,
        boolean productionReady,
        Map<String, String> defaultClaims,
        Map<String, String> defaultEndpoints
) {
}
