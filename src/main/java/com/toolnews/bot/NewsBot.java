package com.toolnews.bot;

import com.toolnews.bot.command.CreateSettingCommandHandler;
import com.toolnews.bot.command.HelpCommandHandler;
import com.toolnews.bot.command.ListOfSettingsCommandHandler;
import com.toolnews.bot.command.StartCommandHandler;
import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.LastCommandState;
import com.toolnews.bot.entity.enumeration.SettingState;
import com.toolnews.bot.repository.SiteSettingRepository;
import com.toolnews.bot.scheduler.SchedulerManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    public static final long CHAT_ID = 701705313L;
    private static final long TARGET_GROUP_CHAT_ID = -4508940743L;

    private final StartCommandHandler startHandler;
    private final CreateSettingCommandHandler createSettingHandler;
    private final ListOfSettingsCommandHandler listOfSettingsCommandHandler;
    private final HelpCommandHandler helpHandler;

    private SettingState settingState;
    private static LastCommandState lastCommandState = LastCommandState.WITHOUT;

    private final TelegramClient client;
    private final SiteSettingRepository siteSettingRepository;
    private final SchedulerManager schedulerManager;

    @Override
    public String getBotToken() {
        return "7414142924:AAFusHnSFZ71AzOY6I-82sYysFQdqh6uSXA";
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    private boolean isUserAllowed(Long userId) {
        return userId.equals(CHAT_ID);
    }


    // --------------------------------------------------------------------------------------------------


    public void setLastCommandState(LastCommandState lastCommand) {
        lastCommandState = lastCommand;
    }

    public void sendMessage(SendMessage message) {

        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            log.error("An error occurred while trying to send a SendMessage. Stacktrace = {}",
                    e.getMessage());
        }

    }

    public void sendText(String text) {

        SendMessage send = SendMessage.builder()
                .chatId(CHAT_ID)
                .text(text)
                .build();
        sendMessage(send);

    }

    @PostConstruct
    public void registerGeneralCommands() {

        SetMyCommands commands = new SetMyCommands(
                Arrays.asList(
                        new BotCommand("/create_setting",
                                "Создать связку настроек"),
                        new BotCommand("/list_of_settings",
                                "Посмотреть существующие"),
                        new BotCommand("/help",
                                "Помощь")
                ),
                new BotCommandScopeDefault(),
                null
        );

        try {
            client.execute(commands);
        } catch (TelegramApiException e) {
            log.error("An error occurred while trying to send a SetMyCommands. Stacktrace = {}",
                    e.getMessage());
        }

        List<SiteSettingEntity> allSettings = siteSettingRepository.findAll();
        if (!allSettings.isEmpty()) {
            for (SiteSettingEntity setting : allSettings) {
                schedulerManager.runThisSettingInScheduler(setting);
            }
        }

    }

    @Override
    public void consume(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            Long chatId = update.getMessage().getChatId();
            if (isUserAllowed(chatId)) {

                String messageText = update.getMessage().getText();
                switch (messageText) {
                    case "/start" -> {

                        lastCommandState = LastCommandState.START;
                        startHandler.handle(this);
                        return;

                    }
                    case "/create_setting" -> {

                        lastCommandState = LastCommandState.CREATE_SETTING;
                        createSettingHandler.resetState();
                        createSettingHandler.handle(this);
                        return;

                    }
                    case "/list_of_settings" -> {

                        lastCommandState = LastCommandState.LIST_OF_SETTINGS;
                        listOfSettingsCommandHandler.handle(this);

                    }
                    case "/help" -> {

                        lastCommandState = LastCommandState.HELP;
                        helpHandler.handle(this);
                        return;

                    }
                }

                if (lastCommandState == LastCommandState.CREATE_SETTING) {

                    createSettingHandler.fillSiteSettings(this, update);

                }


            } else {

                long wrongChatId = update.getMessage().getChatId();

                LeaveChat leaveChat = LeaveChat
                        .builder()
                        .chatId(wrongChatId)
                        .build();

                try {
                    client.execute(leaveChat);
                } catch (TelegramApiException e) {
                    log.error("An error occurred while trying to send a LeaveChat. Stacktrace = {}",
                            e.getMessage());
                }

            }

        } else if (update.hasCallbackQuery()) {

            if (lastCommandState == LastCommandState.CREATE_SETTING) {
                createSettingHandler.fillSiteSettings(this, update);
            }

        }

    }

}