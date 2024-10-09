package com.news.content.checker.command;

import com.news.content.checker.BotUtils;
import com.news.content.checker.entity.SiteSettingEntity;
import com.news.content.checker.entity.enumeration.ListOfSettingsState;
import com.news.content.checker.entity.enumeration.TimeSettingOption;
import com.news.content.checker.repository.SiteSettingRepository;
import com.news.content.checker.scheduler.SchedulerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.news.content.checker.NewsBot.CHAT_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListOfSettingsCommandHandler implements CommandHandler {

    private final SiteSettingRepository repository;
    private final SchedulerManager schedulerManager;

    private SiteSettingEntity lastChoicedSetting;

    public static ListOfSettingsState state = ListOfSettingsState.WITHOUT;

    @Override
    public SendMessage handle() {

        List<SiteSettingEntity> settings = repository.findAll();

        if (settings.isEmpty()) {
            return SendMessage.builder()
                    .chatId(CHAT_ID)
                    .text("""
                            На данный момент нет настроенных связок 🤔
                            """)
                    .build();
        }

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

        state = ListOfSettingsState.WAITING_TAP_FOR_LINK;

        return SendMessage
                .builder()
                .text(listOfSettingsCommandText)
                .chatId(CHAT_ID)
                .replyMarkup(markup)
                .build();

    }

    public SendMessage showSettingsLink(String settingIdString) {

        long settingId = Long.parseLong(settingIdString);
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

        InlineKeyboardButton deleteSchedulerButton = InlineKeyboardButton
                .builder()
                .text("Удалить")
                .callbackData("delete_scheduler")
                .build();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(deleteSchedulerButton);

        InlineKeyboardMarkup markup = InlineKeyboardMarkup
                .builder()
                .keyboardRow(row1)
                .build();

        state = ListOfSettingsState.WAITING_CHOICE_SETTING_BUTTONS;

        return SendMessage
                .builder()
                .chatId(CHAT_ID)
                .text(showSetting)
                .replyMarkup(markup)
                .build();


    }

    public SendMessage ButtonPressed(String data) {

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

        SendMessage sendMessage = null;

        if (data.equals("delete_scheduler")) {

            schedulerManager.stopThisSettingInScheduler(lastChoicedSetting);
            repository.delete(lastChoicedSetting);

            String deleteSchedulerText = """
                    Связка настроек для сайта %s удалена 🧹
                    """
                    .formatted(
                            hostUrl
                    );

            state = ListOfSettingsState.WITHOUT;

            sendMessage = SendMessage.builder()
                    .chatId(CHAT_ID)
                    .text(deleteSchedulerText)
                    .build();

        }

        return sendMessage;

    }

}