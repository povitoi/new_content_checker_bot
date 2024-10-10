package com.toolnews.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class BotConfig {

    @Bean
    public TelegramClient telegramClient() {
        return new OkHttpTelegramClient("7414142924:AAFusHnSFZ71AzOY6I-82sYysFQdqh6uSXA");
    }

//    @Bean
//    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
//        return new ScheduledThreadPoolExecutor(10);
//    }

}
