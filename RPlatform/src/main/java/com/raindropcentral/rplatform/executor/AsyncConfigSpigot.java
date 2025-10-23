/*
package com.raindropcentral.api.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AsyncConfig {

    @Bean
    public ExecutorService executorService() {
        int corePoolSize = 10;
        int maxPoolSize = 50;
        long keepAliveTime = 60L;
        int queueCapacity = 100;

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new ThreadFactory() {
                private int threadCount = 0;
                
                @Override
                public Thread newThread(@NonNull Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setName("jehibernate-pool-" + threadCount++);
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
		
        executor.prestartAllCoreThreads();
        
        return executor;
    }
    
    maybe a idea for spigot / paper...
}*/
