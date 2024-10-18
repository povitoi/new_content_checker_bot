package com.toolnews.bot.command;

import com.toolnews.bot.BotUtils;
import com.toolnews.bot.NewsBot;
import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.ListOfSettingsLastCallbackQuery;
import com.toolnews.bot.entity.enumeration.TimeSettingOption;
import com.toolnews.bot.repository.SiteSettingRepository;
import com.toolnews.bot.scheduler.SchedulerManager;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.toolnews.bot.NewsBot.CHAT_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListOfSettingsCommandHandler implements CommandHandler {

    private final SiteSettingRepository repository;
    private final SchedulerManager schedulerManager;

    private SiteSettingEntity lastChoicedSetting;

    @Setter
    private ListOfSettingsLastCallbackQuery lastCallback = ListOfSettingsLastCallbackQuery.WITHOUT;

    @Override
    public void handle(NewsBot bot) {

        List<SiteSettingEntity> settings = repository.findAll();

        if (settings.isEmpty()) {
            String listIsEmpty = """
                    На данный момент нет настроенных связок 🤔
                    """;
            bot.sendText(listIsEmpty);
            return;
        }

        lastCallback = ListOfSettingsLastCallbackQuery.WAITING_TAP_FOR_LINK;

        String listOfSettingsCommandText = """
                Ваш список настроек 📋
                """;

        List<InlineKeyboardRow> rows = new ArrayList<>();

        int i = 1;
        for (SiteSettingEntity setting : settings) {

            try {
                String hostUrl = new URL(setting.getListUrl()).getHost();
                String settingId = setting.getId().toString();

                InlineKeyboardButton button = InlineKeyboardButton
                        .builder()
                        .text(i++ + ". " + hostUrl)
                        .callbackData(settingId)
                        .build();

                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(button);
                rows.add(row);

            } catch (MalformedURLException e) {
                log.error("""
                                An error occurred while trying to convert
                                the link to a host  in handle() method. Stacktrace = {}
                                """
                        , e.getMessage());
            }

        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);

        SendMessage list = SendMessage
                .builder()
                .text(listOfSettingsCommandText)
                .chatId(CHAT_ID)
                .replyMarkup(markup)
                .build();
        bot.sendMessage(list);

        lastCallback = ListOfSettingsLastCallbackQuery.OUTPUT_SETTINGS;

    }

    public void showSettingsLink(NewsBot bot, Update update) {

        if (update.hasCallbackQuery() &&
                lastCallback == ListOfSettingsLastCallbackQuery.OUTPUT_SETTINGS) {

            long settingId = Long.parseLong(update.getCallbackQuery().getData());
            lastChoicedSetting = repository.findById(settingId).orElse(null);

            LocalDateTime created = lastChoicedSetting.getSettingCreated().toLocalDateTime();
            String hostUrl = null;
            try {
                hostUrl = new URL(lastChoicedSetting.getListUrl()).getHost();
            } catch (MalformedURLException e) {
                log.error("""
                                An error occurred while trying to convert the link
                                to a host in showSettingsLink(NewsBot bot, Update update) method. Stacktrace = {}
                                """
                        , e.getMessage());
            }

            bot.sendMessage(EditMessageReplyMarkup
                    .builder()
                    .chatId(CHAT_ID)
                    .messageId(update.getCallbackQuery().getMessage().getMessageId())
                    .replyMarkup(InlineKeyboardMarkup.builder().build())
                    .build());

            String showSetting = """
                    Связка настроек для сайта:
                    %s
                    
                    Создана:
                    %s в %s
                    
                    Режим проверки:
                    """
                    .formatted(
                            hostUrl,
                            BotUtils.formatDate(created.toLocalDate()),
                            BotUtils.formatTime(created.toLocalTime())
                    );

            if (lastChoicedSetting.getTimeSettingOption() == TimeSettingOption.TIME_OF_DAY) {

                Map<String, String> duration = BotUtils.getTimeDurationUntilNextCheck(
                        lastChoicedSetting.getLastCheck().toLocalDateTime(),
                        lastChoicedSetting.getNewsCheckTime().toLocalTime()
                );

                showSetting = showSetting + """
                        - Каждый день
                        - в %s
                        
                        Следующая проверка:
                        через %s ч. %s мин.
                        """
                        .formatted(
                                BotUtils.formatTime(
                                        lastChoicedSetting.getNewsCheckTime().toLocalTime()),
                                duration.get("hours"),
                                duration.get("minutes")
                        );

            } else {

                showSetting = showSetting + """
                        - Периодический
                        - %s
                        
                        Следующая проверка
                        %s
                        """
                        .formatted(
                                BotUtils.prepareStringForIntervalOption(
                                        lastChoicedSetting.getIntervalUnit(),
                                        lastChoicedSetting.getEveryTimeValue()
                                ),
                                BotUtils.prepareStringForNextCheck(
                                        lastChoicedSetting.getLastCheck().toLocalDateTime(),
                                        lastChoicedSetting.getIntervalUnit(),
                                        lastChoicedSetting.getEveryTimeValue()
                                )
                        );

            }

            InlineKeyboardButton stopSchedulerButton = InlineKeyboardButton
                    .builder()
                    .text("Приостановить")
                    .callbackData("stop_scheduler")
                    .build();

            InlineKeyboardButton startSchedulerButton = InlineKeyboardButton
                    .builder()
                    .text("Запустить")
                    .callbackData("start_scheduler")
                    .build();

            InlineKeyboardButton deleteSchedulerButton = InlineKeyboardButton
                    .builder()
                    .text("Удалить")
                    .callbackData("delete_scheduler")
                    .build();

            InlineKeyboardRow row1 = new InlineKeyboardRow();
            row1.add(deleteSchedulerButton);

            InlineKeyboardRow row2 = new InlineKeyboardRow();
            if (lastChoicedSetting.isRunning())
                row2.add(stopSchedulerButton);
            else
                row2.add(startSchedulerButton);

            InlineKeyboardMarkup markup = InlineKeyboardMarkup
                    .builder()
                    .keyboardRow(row1)
                    .keyboardRow(row2)
                    .build();

            SendMessage message = SendMessage
                    .builder()
                    .chatId(CHAT_ID)
                    .text(showSetting)
                    .replyMarkup(markup)
                    .build();
            bot.sendMessage(message);

            lastCallback = ListOfSettingsLastCallbackQuery.WAITING_CHOICE_SETTING_BUTTONS;

        } else if (update.hasCallbackQuery() &&
                lastCallback == ListOfSettingsLastCallbackQuery.WAITING_CHOICE_SETTING_BUTTONS) {

            String hostUrl = null;
            try {
                hostUrl = new URL(lastChoicedSetting.getListUrl()).getHost();
            } catch (MalformedURLException e) {
                log.error("""
                                An error occurred while trying to convert the link
                                to a host in showSettingsLink(
                                NewsBot bot, Update update) method in callbackquery. Stacktrace = {}
                                """
                        , e.getMessage());
            }

            if (update.getCallbackQuery().getData().equals("stop_scheduler")) {

                schedulerManager.stopThisSettingInScheduler(lastChoicedSetting);
                lastChoicedSetting.setRunning(false);
                repository.save(lastChoicedSetting);

                bot.sendMessage(EditMessageReplyMarkup
                        .builder()
                        .chatId(CHAT_ID)
                        .messageId(update.getCallbackQuery().getMessage().getMessageId())
                        .replyMarkup(InlineKeyboardMarkup.builder().build())
                        .build());


                bot.sendText("""
                        Проверка новостей с сайта %s приостановлена ⏸️
                        """
                        .formatted(
                                hostUrl
                        )
                );

            } else if (update.getCallbackQuery().getData().equals("start_scheduler")) {

                schedulerManager.runThisSettingInScheduler(lastChoicedSetting);
                lastChoicedSetting.setRunning(true);
                repository.save(lastChoicedSetting);

                bot.sendMessage(EditMessageReplyMarkup
                        .builder()
                        .chatId(CHAT_ID)
                        .messageId(update.getCallbackQuery().getMessage().getMessageId())
                        .replyMarkup(InlineKeyboardMarkup.builder().build())
                        .build());

                bot.sendText("""
                        Проверка новостей с сайта %s запущена ▶️
                        """
                        .formatted(
                                hostUrl
                        )
                );

            } else if (update.getCallbackQuery().getData().equals("delete_scheduler")) {

                schedulerManager.stopThisSettingInScheduler(lastChoicedSetting);
                repository.delete(lastChoicedSetting);

                bot.sendText(
                        """
                                Связка настроек для сайта %s полностью удалена 🧹
                                """
                                .formatted(
                                        hostUrl
                                )
                );

            }

        } else if (!update.hasCallbackQuery()) {
            String invalidTimeUnit = """
                    Пожалуйста, выберите значение по кнопкам 🙂
                    """;
            bot.sendText(invalidTimeUnit);
        }

    }
}