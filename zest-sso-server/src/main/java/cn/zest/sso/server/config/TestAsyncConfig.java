package cn.zest.sso.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * 测试环境同步执行异步任务，便于集成测试断言副作用。
 */
@Configuration
@EnableAsync
@Profile("test")
public class TestAsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return Runnable::run;
    }
}
