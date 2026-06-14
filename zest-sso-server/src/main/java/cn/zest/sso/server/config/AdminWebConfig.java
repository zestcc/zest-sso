package cn.zest.sso.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Admin SPA 静态资源与前端路由回退。
 */
@Configuration
public class AdminWebConfig implements WebMvcConfigurer {

    private static final String ADMIN_INDEX = "/static/admin/index.html";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        if (shouldFallbackToIndex(resourcePath)) {
                            return new ClassPathResource(ADMIN_INDEX);
                        }
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable() && requested.isFile()) {
                            return requested;
                        }
                        return new ClassPathResource(ADMIN_INDEX);
                    }
                });
    }

    private static boolean shouldFallbackToIndex(String resourcePath) {
        return !StringUtils.hasText(resourcePath)
                || ".".equals(resourcePath)
                || resourcePath.endsWith("/")
                || !resourcePath.contains(".");
    }
}
