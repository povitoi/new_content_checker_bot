package com.toolnews.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@SpringBootApplication
@EnableScheduling
public class ToolMastersBotApplication {

	public static void main(String[] args) {

		ApplicationContext context = SpringApplication.run(ToolMastersBotApplication.class, args);

		NewsBot newsBot = context.getBean(NewsBot.class);

		try (var botsApplication = new TelegramBotsLongPollingApplication()) {
			String botToken = "7414142924:AAFusHnSFZ71AzOY6I-82sYysFQdqh6uSXA";
			botsApplication.registerBot(botToken, newsBot);
		} catch (Exception e) {
            throw new RuntimeException(e);
        }

	}

}
