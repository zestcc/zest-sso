package cn.zest.sso.server.support;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 集成测试依赖本机 MySQL，运行前请设置环境变量 MYSQL_PASSWORD。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnabledIfEnvironmentVariable(named = "MYSQL_PASSWORD", matches = ".+")
public @interface RequiresMysql {
}
