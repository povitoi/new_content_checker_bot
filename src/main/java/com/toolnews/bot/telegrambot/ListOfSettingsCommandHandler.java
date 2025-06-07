package com.toolnews.bot.telegrambot;

import com.toolnews.bot.model.SiteSettingEntity;
import com.toolnews.bot.model.ListOfSettingsState;
import com.toolnews.bot.model.TimeSettingOption;
import com.toolnews.bot.repository.SiteSettingRepository;
import com.toolnews.bot.service.SchedulerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.toolnews.bot.telegrambot.NewsBot.CHAT_ID;

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

            String hostUrl = BotUtils.toUrl(setting.getListUrl()).getHost();
            String settingId = setting.getId().toString();

            String text = i++ + ". " + hostUrl;
            if (!setting.isRunning())
                text = text + " %s".formatted("⚠️");

            InlineKeyboardButton button = InlineKeyboardButton
                    .builder()
                    .text(text)
                    .callbackData(settingId)
                    .build();

            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(button);
            rows.add(row);

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
        String hostUrl = BotUtils.toUrl(lastChoicedSetting.getListUrl()).getHost();

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

        if (!lastChoicedSetting.isRunning()) {

            showSetting = showSetting + """
                    
                    Проверка этого сайта приостановлена по техническим причинам. Пожалуйста, удалите эту связку настроек и создайте заново.
                    """;

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

        String hostUrl = BotUtils.toUrl(lastChoicedSetting.getListUrl()).getHost();

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