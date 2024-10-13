package com.toolnews.bot.config;

import com.toolnews.bot.scheduler.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class BotConfig {

    @Bean
    public TelegramClient telegramClient() {
        return new OkHttpTelegramClient("7414142924:AAFusHnSFZ71AzOY6I-82sYysFQdqh6uSXA");
    }

    @Bean
    public ScheduledExecutorService newScheduledThreadPool() {
        return Executors.newScheduledThreadPool(20);
    }

    @Bean
    public TaskScheduler scheduledTaskRegistrar() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(20);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public ConcurrentHashMap<Long, Scheduler> schedulerMap() {
        return new ConcurrentHashMap<>();
    }

//    @Override
//    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
//        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(20));
//    }
}
