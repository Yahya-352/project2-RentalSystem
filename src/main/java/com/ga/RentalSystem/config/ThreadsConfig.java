package com.ga.RentalSystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ThreadsConfig {

    @Bean(name = "emailExecutor")
    public ThreadPoolTaskExecutor emailExecutor() {
        return createExecutor("email-", 2, 4, 100,
                new ThreadPoolExecutor.CallerRunsPolicy(), true);
    }

    @Bean(name = "notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        return createExecutor("notification-", 2, 4, 100,
                new ThreadPoolExecutor.DiscardPolicy(), false);
    }

    private ThreadPoolTaskExecutor createExecutor(String prefix, int core, int max, int queue,
                                                  RejectedExecutionHandler rejection,
                                                  boolean waitOnShutdown) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setRejectedExecutionHandler(rejection);
        executor.setWaitForTasksToCompleteOnShutdown(waitOnShutdown);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

}
