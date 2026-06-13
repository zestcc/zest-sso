package cn.zest.sso.server.security;

import cn.zest.sso.common.constant.SsoConstants;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * OAuth2 Token 自定义器，注入 Zest 生态标准 Claims。
 */
@Component
public class SsoTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (!"access_token".equals(context.getTokenType().getValue())
                && !"id_token".equals(context.getTokenType().getValue())) {
            return;
        }

        Object principal = context.getPrincipal().getPrincipal();
        if (!(principal instanceof SsoUserDetails userDetails)) {
            return;
        }

        context.getClaims()
                .claim(SsoConstants.CLAIM_USER_ID, userDetails.getUserId())
                .claim(SsoConstants.CLAIM_USERNAME, userDetails.getUsername())
                .claim(SsoConstants.CLAIM_EMAIL, userDetails.getEmail())
                .claim(SsoConstants.CLAIM_NAME, userDetails.getDisplayName())
                .claim(SsoConstants.CLAIM_ROLES, userDetails.getRoles());

        if (!userDetails.getGroups().isEmpty()) {
            context.getClaims().claim(SsoConstants.CLAIM_GROUPS, userDetails.getGroups());
        }

        if (userDetails.getDefaultTenantId() != null) {
            context.getClaims().claim(SsoConstants.CLAIM_TENANT_ID, userDetails.getDefaultTenantId());
        }
    }
}
