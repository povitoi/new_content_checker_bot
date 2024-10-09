package com.toolnews.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
public class ToolMastersBotApplication {

	public static void main(String[] args) {
		try (var botsApplication = new TelegramBotsLongPollingApplication()) {
			String botToken = "7414142924:AAFusHnSFZ71AzOY6I-82sYysFQdqh6uSXA";
			botsApplication.registerBot(botToken, new NewsBot());
		} catch (Exception e) {
            throw new RuntimeException(e);
        }
        SpringApplication.run(ToolMastersBotApplication.class, args);
	}

}
