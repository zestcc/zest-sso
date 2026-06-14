package cn.zest.sso.server.security.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * OAuth2 JDBC 授权记录中的 Long 型 claim（如 userId、tenantId）反序列化白名单。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public abstract class LongMixin {
}
