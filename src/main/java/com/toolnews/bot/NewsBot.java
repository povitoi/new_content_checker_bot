package com.toolnews.bot;

import com.toolnews.bot.command.CreateSettingCommandHandler;
import com.toolnews.bot.command.HelpCommandHandler;
import com.toolnews.bot.command.ListOfSettingsCommandHandler;
import com.toolnews.bot.command.StartCommandHandler;
import com.toolnews.bot.config.ApplicationPropertiesConfig;
import com.toolnews.bot.entity.BotSettingsEntity;
import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.BotCommandState;
import com.toolnews.bot.entity.enumeration.CreateSettingState;
import com.toolnews.bot.entity.enumeration.ListOfSettingsState;
import com.toolnews.bot.repository.BotSettingsRepository;
import com.toolnews.bot.repository.SiteSettingRepository;
import com.toolnews.bot.scheduler.SchedulerManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final ApplicationPropertiesConfig applicationPropertiesConfig;

    public static Long CHAT_ID;
    public static String zonedId;
    public static Long TARGET_GROUP_CHAT_ID = null;

    private final StartCommandHandler startHandler;
    private final CreateSettingCommandHandler createSettingHandler;
    private final ListOfSettingsCommandHandler listOfSettingsHandler;
    private final HelpCommandHandler helpHandler;

    public static BotCommandState botCommandState = BotCommandState.WITHOUT;

    private final TelegramClient client;
    private final SiteSettingRepository siteSettingRepository;
    private final BotSettingsRepository botSettingsRepository;
    private final SchedulerManager schedulerManager;

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    private boolean isUserAllowed(Long userId) {
        return userId.equals(CHAT_ID);
    }


    // --------------------------------------------------------------------------------------------------


    private void sendText(String text, long chatId) {

        SendMessage send = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        sendMessage(send);

    }

    private void startAllRunningSchedulers() {

        List<SiteSettingEntity> allSettings = siteSettingRepository.findAll();
        if (!allSettings.isEmpty()) {
            for (SiteSettingEntity setting : allSettings) {
                if (setting.isRunning())
                    schedulerManager.runThisSettingInScheduler(setting);
            }
        }

    }

    private void stopAllRunningSchedulers() {

        List<SiteSettingEntity> allSettings = siteSettingRepository.findAll();
        if (!allSettings.isEmpty()) {
            for (SiteSettingEntity setting : allSettings) {
                if (setting.isRunning())
                    schedulerManager.stopThisSettingInScheduler(setting);
            }
        }

    }

    @PostConstruct
    public void startInit() {

        CHAT_ID = Long.parseLong(applicationPropertiesConfig.getAllowedChatId());
        zonedId = applicationPropertiesConfig.getZonedId();

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

        sendMessage(commands);

        BotSettingsEntity botSettings = botSettingsRepository.findFirstByOrderByIdAsc().orElse(null);
        if (botSettings != null) {
            Long groupId = botSettings.getGroupId();
            if (groupId != null) {
                TARGET_GROUP_CHAT_ID = botSettings.getGroupId();
                startAllRunningSchedulers();
            }
        }


    }

    private void sendMessage(Object message) {

        try {

            if (message instanceof SendMessage send) {

                if (!send.getText().isEmpty())
                    client.execute((SendMessage) message);

            } else if (message instanceof EditMessageReplyMarkup) {

                client.execute((EditMessageReplyMarkup) message);

            } else if (message instanceof LeaveChat) {

                client.execute((LeaveChat) message);

            } else if (message instanceof SetMyCommands) {

                client.execute((SetMyCommands) message);

            }

        } catch (TelegramApiException e) {
            log.error("An error occurred while trying to send a SendMessage. Stacktrace = {}",
                    e.getMessage());
        }

    }

    private boolean botNotAddedToGroup() {

        BotSettingsEntity botSettings = botSettingsRepository.findFirstByOrderByIdAsc().orElse(null);
        if (botSettings == null) {
            return true;
        } else {
            Long groupId = botSettings.getGroupId();
            return groupId == null;
        }

    }

    @Override
    @Transactional
    public void consume(Update update) {

        long fromId = 0L;
        long communityId = 0L;

        if (update.hasMessage()) {
            fromId = update.getMessage().getFrom().getId();
            if (!update.getMessage().getChat().isUserChat())
                return;
        } else if (update.hasCallbackQuery()) {
            fromId = update.getCallbackQuery().getFrom().getId();
        } else if (update.hasMyChatMember()) {
            fromId = update.getMyChatMember().getFrom().getId();
            communityId = update.getMyChatMember().getChat().getId();
        }

        boolean userAllowed = isUserAllowed(fromId);

        if (userAllowed) {

            if (update.hasMessage()) {
                // Update is text

                String text = update.getMessage().getText();

                if (text.startsWith("/")) {
                    // Text is command

                    switch (text) {
                        case "/start" -> {

                            botCommandState = BotCommandState.START;
                            sendText(startHandler.handle(), fromId);

                        }
                        case "/create_setting" -> {

                            if (botNotAddedToGroup()) {
                                sendText("""
                                        Бот не добавлен ни в одну группу 🫤
                                        Пожалуйста, сделайте это, чтобы команды начали работать.
                                        """, fromId);
                                return;
                            }

                            botCommandState = BotCommandState.CREATE_SETTING;
                            createSettingHandler.resetStateSettingCreation();

                            String createdText = createSettingHandler.handle();
                            sendText(createdText, fromId);

                            BotUtils.stopThread(500);

                            CreateSettingCommandHandler.state = CreateSettingState.WAITING_LIST_URL;
                            String waitingListUrlText = createSettingHandler.handle();
                            sendText(waitingListUrlText, fromId);

                        }
                        case "/list_of_settings" -> {

                            if (botNotAddedToGroup()) {
                                sendText("""
                                        Бот не добавлен ни в одну группу 🫤
                                        Пожалуйста, сделайте это, чтобы команды начали работать.
                                        """, fromId);
                                return;
                            }

                            botCommandState = BotCommandState.LIST_OF_SETTINGS;
                            sendMessage(listOfSettingsHandler.handle());

                        }
                        case "/help" -> {

                            botCommandState = BotCommandState.HELP;
                            sendText(helpHandler.handle(), fromId);

                        }
                        default -> {

                            sendText("""
                                    Пожалуйста, введите одну из доступных команд 🙂
                                    """, fromId);

                        }
                    }

                } else {
                    // Text is not command

                    boolean waitingButtonPressed =
                            botCommandState == BotCommandState.CREATE_SETTING &&
                                    CreateSettingCommandHandler.state == CreateSettingState.WAITING_TIME_UNIT;

                    if (waitingButtonPressed) {
                        sendText("""
                                Пожалуйста, выберите значение по кнопкам 🙂
                                """, fromId);
                        return;
                    }

                    if (botCommandState == BotCommandState.CREATE_SETTING) {

                        switch (CreateSettingCommandHandler.state) {

                            case WAITING_LIST_URL -> {

                                String fillListUrlResponse = createSettingHandler.fillListUrl(text);
                                if (fillListUrlResponse.isEmpty()) {

                                    CreateSettingCommandHandler.state =
                                            CreateSettingState.WAITING_LAST_ELEMENT_URL;

                                    fillListUrlResponse = createSettingHandler.handle();
                                    sendText(fillListUrlResponse, fromId);

                                } else {

                                    // invalidUrl
                                    sendText(fillListUrlResponse, fromId);

                                }

                            }

                            case WAITING_LAST_ELEMENT_URL -> {

                                String fillLastElementUrlResponse = createSettingHandler.fillLastElementUrl(text);
                                if (fillLastElementUrlResponse.isEmpty()) {

                                    CreateSettingCommandHandler.state =
                                            CreateSettingState.WAITING_TIME;

                                    fillLastElementUrlResponse = createSettingHandler.handle();
                                    sendText(fillLastElementUrlResponse, fromId);

                                } else {

                                    // invalidUrl
                                    sendText(fillLastElementUrlResponse, fromId);

                                }

                            }

                            case WAITING_TIME -> {

                                String fillTimeResponse = createSettingHandler.fillTime(text);
                                if (fillTimeResponse.equals("0")) {

                                    CreateSettingCommandHandler.state =
                                            CreateSettingState.READY;
                                    botCommandState = BotCommandState.WITHOUT;
                                    createSettingHandler.settingIsReady();

                                    fillTimeResponse = createSettingHandler.handle();
                                    sendText(fillTimeResponse, fromId);

                                } else if (fillTimeResponse.equals("1")) {

                                    CreateSettingCommandHandler.state =
                                            CreateSettingState.WAITING_TIME_UNIT;

                                    fillTimeResponse = createSettingHandler.handle();
                                    InlineKeyboardMarkup keyboardMarkup =
                                            createSettingHandler.getKeyboardForState();

                                    sendMessage(SendMessage.builder()
                                            .chatId(fromId)
                                            .text(fillTimeResponse)
                                            .replyMarkup(keyboardMarkup)
                                            .build()
                                    );

                                }

                            }

                        }

                    } else {

                        sendText("""
                                Пожалуйста, введите одну из доступных команд 🙂
                                """, fromId);

                    }

                }

            } else if (update.hasCallbackQuery()) {
                // Update is click of button

                CallbackQuery callbackQuery = update.getCallbackQuery();

                if (botCommandState == BotCommandState.CREATE_SETTING) {

                    if (CreateSettingCommandHandler.state == CreateSettingState.WAITING_TIME_UNIT) {

                        createSettingHandler.fillTimeUnit(callbackQuery.getData());

                        CreateSettingCommandHandler.state =
                                CreateSettingState.READY;
                        botCommandState = BotCommandState.WITHOUT;
                        createSettingHandler.settingIsReady();

                        String fillTimeUnitResponse = createSettingHandler.handle();
                        sendText(fillTimeUnitResponse, fromId);

                        CreateSettingCommandHandler.state =
                                CreateSettingState.CREATED;

                        sendMessage(EditMessageReplyMarkup
                                .builder()
                                .chatId(fromId)
                                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                                .replyMarkup(InlineKeyboardMarkup.builder().build())
                                .build());

                    }

                } else if (botCommandState == BotCommandState.LIST_OF_SETTINGS) {

                    sendMessage(EditMessageReplyMarkup
                            .builder()
                            .chatId(fromId)
                            .messageId(update.getCallbackQuery().getMessage().getMessageId())
                            .replyMarkup(InlineKeyboardMarkup.builder().build())
                            .build());

                    if (ListOfSettingsCommandHandler.state == ListOfSettingsState.WAITING_TAP_FOR_LINK) {

                        sendMessage(listOfSettingsHandler.showSettingsLink(callbackQuery.getData()));

                    } else if (ListOfSettingsCommandHandler.state ==
                            ListOfSettingsState.WAITING_CHOICE_SETTING_BUTTONS) {

                        sendMessage(listOfSettingsHandler.ButtonPressed(callbackQuery.getData()));

                    }

                }

            } else if (update.hasMyChatMember()) {

                BotSettingsEntity botSettings = botSettingsRepository.findFirstByOrderByIdAsc()
                        .orElse(null);
                String chatMemberStatus = update.getMyChatMember().getNewChatMember().getStatus();
                String chatTitle = update.getMyChatMember().getChat().getTitle();

                if (chatMemberStatus.equals("left")) {

                    if (botSettings != null) {
                        botSettings.setGroupId(null);
                        botSettingsRepository.save(botSettings);

                        stopAllRunningSchedulers();

                        sendText("""
                                Вы исключили бота из группы %s
                                Все связки настроек приостановлены 🫤
                                """.formatted(chatTitle), fromId);
                    }

                } else if (chatMemberStatus.equals("member")) {

                    if (botSettings != null) {

                        Long oldGroupId = botSettings.getGroupId();
                        if (oldGroupId != null) {
                            sendMessage(LeaveChat
                                    .builder()
                                    .chatId(oldGroupId)
                                    .build()
                            );
                        }

                        botSettings.setGroupId(communityId);
                        botSettingsRepository.save(botSettings);

                    } else {

                        botSettings = new BotSettingsEntity();
                        botSettings.setGroupId(communityId);
                        botSettingsRepository.save(botSettings);

                    }

                    TARGET_GROUP_CHAT_ID = communityId;
                    startAllRunningSchedulers();

                    sendText("""
                            Вы добавили бота в группу %s
                            Планировщик запущен 🙂
                            """.formatted(chatTitle), fromId);

                }

            }

        } else {
            // Not allowed user

            if (communityId != 0L) {
                // Bot it was added in group or channel
                sendMessage(LeaveChat
                        .builder()
                        .chatId(communityId)
                        .build()
                );
            }

            sendMessage(SendMessage.builder()
                    .chatId(fromId)
                    .text("Вы не можете взаимодействовать с этим ботом.")
                    .build()
            );

        }

    }

}
