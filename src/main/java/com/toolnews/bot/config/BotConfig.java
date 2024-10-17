package com.toolnews.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Configuration
public class BotConfig {

    @Bean
    public TelegramClient telegramClient() {
        return new OkHttpTelegramClient("7414142924:AAFusHnSFZ71AzOY6I-82sYysFQdqh6uSXA");
    }

    @Bean
    public TaskScheduler newScheduledThreadPool() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(30);
        return taskScheduler;
    }

    @Bean
    public ConcurrentHashMap<Long, ScheduledFuture<?>> concurrentHashMap() {
        return new ConcurrentHashMap<>();
    }

}
