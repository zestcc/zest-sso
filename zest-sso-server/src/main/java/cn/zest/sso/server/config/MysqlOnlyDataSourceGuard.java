package cn.zest.sso.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

/**
 * 禁止 H2 等嵌入式数据库，仅允许 MySQL。
 */
@Configuration
public class MysqlOnlyDataSourceGuard {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @PostConstruct
    void rejectNonMysqlDatasource() {
        if (!StringUtils.hasText(datasourceUrl)) {
            return;
        }
        String url = datasourceUrl.toLowerCase();
        if (url.contains(":h2:") || url.contains("jdbc:h2")) {
            throw new IllegalStateException(
                    "H2 已禁用，请配置 MySQL 数据源（spring.datasource.url=jdbc:mysql://...）");
        }
        if (!url.startsWith("jdbc:mysql:")) {
            throw new IllegalStateException(
                    "仅支持 MySQL 数据源，当前 spring.datasource.url=" + datasourceUrl);
        }
    }
}
