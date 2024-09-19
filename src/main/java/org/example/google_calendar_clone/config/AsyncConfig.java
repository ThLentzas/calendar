package org.example.google_calendar_clone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
class AsyncConfig {

    /*
        https://docs.spring.io/spring-framework/reference/integration/scheduling.html
        ThreadPoolTaskExecutor vs ThreadPoolTaskScheduler both extend the TaskExecutor interface

        https://stackoverflow.com/questions/33453722/spring-threadpooltaskscheduler-vs-threadpooltaskexecutor
     */
    @Bean
    Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();

        return executor;
    }
}