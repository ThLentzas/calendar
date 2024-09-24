package org.example.calendar.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/*
    https://docs.spring.io/spring-framework/reference/integration/scheduling.html

    By default, scheduled tasks run on a single thread. If we have multiple tasks that will run roughly at the same
    time, the current task will block the thread and the other tasks will have to wait until the current one is over
    We need to increase the threads on the thread pool(default is 1). For integration tests we want to disable the
    scheduling process. We do that by enabling it only if there is a property present. When we don't have the property
    in our environment, the scheduling will be enabled thanks to matchIfMissing = true, but we explicitly pass the
    property to disable it.
    https://www.baeldung.com/spring-test-disable-enablescheduling

    For scheduling tasking in a cluster environment where we have multiple instances of the application running at the
    same time, and we want scheduled tasks to happen once https://www.youtube.com/watch?v=92-qLIxv0JA

    Spring also supports Quartz.
    https://docs.spring.io/spring-boot/reference/io/quartz.html
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", matchIfMissing = true)
class SchedulingConfig {

    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("EmailTask-");

        return scheduler;
    }
}
