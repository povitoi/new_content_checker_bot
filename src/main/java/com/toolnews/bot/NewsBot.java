package com.toolnews.bot;

import com.toolnews.bot.command.CreateSettingCommandHandler;
import com.toolnews.bot.command.HelpCommandHandler;
import com.toolnews.bot.command.StartCommandHandler;
import com.toolnews.bot.entity.enumeration.LastCommandState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class NewsBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    public static final long CHAT_ID = 701705313L;
    private static final long TARGET_GROUP_CHAT_ID = -4508940743L;

    private final StartCommandHandler startHandler;
    private final CreateSettingCommandHandler createSettingHandler;
    private final HelpCommandHandler helpHandler;

    private static LastCommandState lastCommandState;

    private final TelegramClient client;

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }

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

    // COMMAND PROCESSING START
    //
    //

    public void sendMessage(SendMessage message) {

        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    public void sendText(String text) {

        SendMessage send = SendMessage.builder()
                .chatId(CHAT_ID)
                .text(text)
                .build();

        sendMessage(send);

    }

    public void setLastCommandState(LastCommandState lastCommand) {
        lastCommandState = lastCommand;
    }

    @PostConstruct
    public void registerGeneralCommands() {

        SetMyCommands commands = new SetMyCommands(
                Arrays.asList(
                        new BotCommand("/create_setting",
                                "Создать связку настроек"),
                        new BotCommand("/settings_list",
                                "Посмотреть существующие"),
                        new BotCommand("/help",
                                "Помощь")
                ),
                new BotCommandScopeDefault(),
                null
        );

//        var setting = SiteSettingEntity.builder()
//                .id(0L)
//                .timeSettingOption(TimeSettingOption.TIME_OF_DAY)
//                .newsCheckTime(Time.valueOf(LocalTime.of(19, 34)))
//                .listUrl("https://dlt.by/novinki-v-nashem-assortimente/")
//                .lastElementUrl("https://dlt.by/brendi/produkciya_dlt/graver-dlt-g-100-art1151-new")
//                .elementWrapper("image")
//                .build();
//        schedulerManager.runThisSettingInScheduler(setting);

        try {
            client.execute(commands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consume(Update update) {

        if (update.hasMessage()) {

            Long chatId = update.getMessage().getChatId();
            if (isUserAllowed(chatId)) {

                String messageText = update.getMessage().getText();
                switch (messageText) {
                    case "/start" -> {

                        startHandler.handle(this);
                        return;

                    }
                    case "/create_setting" -> {

                        createSettingHandler.resetState();
                        createSettingHandler.handle(this);
                        return;

                    }
                    case "/help" -> {

                        helpHandler.handle(this);
                        return;

                    }
                }

                if (lastCommandState == LastCommandState.CREATE_SETTING) {

                    createSettingHandler.fillSiteSettings(this, messageText);

                }


            } else {
                // обработать случай, когда боту написал левый чувак
                // выйти из группы, канала и т д
            }

        }

    }

    //
    //
    // COMMAND PROCESSING END

}