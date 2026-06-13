package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ClientOnboardingVO {
    private String stack;
    private String issuer;
    private List<String> recommendedScopes;
    private List<String> recommendedGrants;
    private String redirectUri;
    private Map<String, String> snippets;
}
