package com.toolnews.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
@EnableScheduling
@Slf4j
@EnableJpaRepositories(basePackages = "com.toolnews.bot.repository")
public class NewContentCheckerBotApplication {

	public static void main(String[] args) {

		ApplicationContext context = SpringApplication.run(NewContentCheckerBotApplication.class, args);

		NewsBot newsBot = context.getBean(NewsBot.class);
		String botToken = context.getEnvironment().getProperty("telegram.bot.token");

		try (var botsApplication = new TelegramBotsLongPollingApplication()) {
			botsApplication.registerBot(botToken, newsBot);
		} catch (TelegramApiException e) {
            log.error("Error when trying to register a bot. Stacktrace = {}", e.getMessage());
        } catch (Exception e) {
			log.error("Error creating object TelegramBotsLongPollingApplication. Stacktrace = {}", e.getMessage());
		}

	}

}
