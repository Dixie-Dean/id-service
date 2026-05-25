package dev.dixie.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class IdServiceConfig {

    @Bean
    public JedisPool jedisPool() {
        var jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setJmxEnabled(false);
        return new JedisPool(jedisPoolConfig, "localhost", 6381);
    }

    @Bean
    public ThreadPoolTaskExecutor executorService() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("id-generator-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
