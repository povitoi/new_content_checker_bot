package com.toolnews.bot.command;

import com.toolnews.bot.BotUtils;
import com.toolnews.bot.NewsBot;
import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.*;
import com.toolnews.bot.repository.SiteSettingRepository;
import com.toolnews.bot.scheduler.SchedulerManager;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.io.IOException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static com.toolnews.bot.NewsBot.CHAT_ID;

@Service
@RequiredArgsConstructor
public class CreateSettingCommandHandler implements CommandHandler {

    private final SiteSettingRepository siteSettingRepository;

    private final SchedulerManager schedulerManager;

    private SettingState state = SettingState.CREATED;
    private String listUrl;
    private String elementUrl;
    private String elementWrapper;
    private LinkType linkType;
    private TimeSettingOption timeSettingOption;
    private IntervalUnit intervalUnit;
    private Time newsCheckTime;
    private Integer everyTimeValue;

    private SiteSettingEntity siteSettingEntity;

    public void resetState() {

        state = SettingState.CREATED;
        listUrl = null;
        elementUrl = null;
        elementWrapper = null;
        linkType = null;
        timeSettingOption = null;
        intervalUnit = null;
        newsCheckTime = null;
        everyTimeValue = null;
        siteSettingEntity = null;

    }

    public void handle(NewsBot bot) {

        String createSettingCommandText = "Создаю связку настроек ⚙️";
        bot.sendText(createSettingCommandText);

        BotUtils.stopThread(500);

        String listUrlRequestText = """
                1. Введите ссылку на страницу новостей ✍️
                """;
        bot.sendText(listUrlRequestText);

        state = SettingState.WAITING_LIST_URL;

    }

    @Transactional
    public void fillSiteSettings(NewsBot bot, Update update) {

        String messageText = "";
        if (update.hasMessage() && update.getMessage().hasText()) {
            messageText = update.getMessage().getText();
        }

        if (state == SettingState.WAITING_LIST_URL) {

            if (invalidUrl(messageText)) {

                String invalidListUrlText = """
                        Неверный формат ссылки 🫢
                        
                        Ссылка должна начинаться на http:// или https://
                        
                        Попробуйте еще раз, это бесплатно 🙂
                        """;
                bot.sendText(invalidListUrlText);
                return;
            }

            listUrl = messageText;

            state = SettingState.WAITING_LAST_ELEMENT_URL;

            String elementUrlRequestText = """
                    2. Введите ссылку на одну из новостей 📝
                    """;
            bot.sendText(elementUrlRequestText);

        } else if (state == SettingState.WAITING_LAST_ELEMENT_URL) {

            if (invalidUrl(messageText)) {

                String invalidElementUrlText = """
                        Неверный формат ссылки 🫢
                        
                        Ссылка должна начинаться на http:// или https://
                        
                        Попробуйте еще раз, это бесплатно 🙂
                        """;
                bot.sendText(invalidElementUrlText);
                return;
            }

            elementUrl = messageText;

            state = SettingState.WAITING_TIME;

            String timeRequestText = """
                    3. Введите время или периодичность проверки ⏰
                    """;
            bot.sendText(timeRequestText);

        } else if (state == SettingState.WAITING_TIME) {

            if (invalidTime(messageText) && invalidInteger(messageText)) {

                String invalidTimeText = """
                        Недопустимый формат 🧐
                        
                        Введите время в 24-часовом формате, либо число не больше 30
                        """;
                bot.sendText(invalidTimeText);
                return;
            }

            timeSettingOption = getTimeSettingOption(messageText);
            if (timeSettingOption == TimeSettingOption.TIME_OF_DAY) {

                newsCheckTime = Time.valueOf(LocalTime.parse(messageText));

                settingIsReady(bot);

            } else {

                everyTimeValue = Integer.parseInt(messageText);
                state = SettingState.WAITING_TIME_UNIT;

                String timeUnitRequestText = """
                        4. Выберите значение периодичности
                        """;
                SendMessage message = SendMessage
                        .builder()
                        .chatId(CHAT_ID)
                        .text(timeUnitRequestText)
                        .replyMarkup(
                                InlineKeyboardMarkup
                                        .builder()
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton
                                                                .builder()
                                                                .text("Час")
                                                                .callbackData("h")
                                                                .build(),
                                                        InlineKeyboardButton
                                                                .builder()
                                                                .text("День")
                                                                .callbackData("d")
                                                                .build())
                                        )
                                        .keyboardRow(
                                                new InlineKeyboardRow(
                                                        InlineKeyboardButton
                                                                .builder()
                                                                .text("Минута")
                                                                .callbackData("m")
                                                                .build()
                                                )
                                        ).build()
                        ).build();

                bot.sendMessage(message);
            }


        } else if (state == SettingState.WAITING_TIME_UNIT) {

            if (update.hasCallbackQuery()) {
                intervalUnit = getIntervalUnit(update.getCallbackQuery());

                bot.sendMessage(EditMessageReplyMarkup
                        .builder()
                        .chatId(CHAT_ID)
                        .messageId(update.getCallbackQuery().getMessage().getMessageId())
                        .replyMarkup(InlineKeyboardMarkup.builder().build())
                        .build());

            } else {
                String invalidTimeUnit = """
                        Пожалуйста, выберите значение по кнопкам 🙂
                        """;
                bot.sendText(invalidTimeUnit);
                return;
            }

            settingIsReady(bot);

        }

    }

    @Transactional
    public void settingIsReady(NewsBot bot) {

        elementWrapper = getElementWrapper(listUrl, elementUrl);

        if (timeSettingOption == TimeSettingOption.TIME_OF_DAY) {
            siteSettingEntity = SiteSettingEntity
                    .builder()
                    .running(true)
                    .settingCreated(Timestamp.valueOf(LocalDateTime.now()))
                    .listUrl(listUrl)
                    .elementUrl(elementUrl)
                    .timeSettingOption(timeSettingOption)
                    .newsCheckTime(newsCheckTime)
                    .lastCheck(Timestamp.valueOf(LocalDateTime.now()))
                    .elementWrapper(elementWrapper)
                    .linkType(linkType)
                    .build();
        } else if (timeSettingOption == TimeSettingOption.INTERVAL) {
            siteSettingEntity = SiteSettingEntity
                    .builder()
                    .running(true)
                    .settingCreated(Timestamp.valueOf(LocalDateTime.now()))
                    .listUrl(listUrl)
                    .elementUrl(elementUrl)
                    .timeSettingOption(timeSettingOption)
                    .intervalUnit(intervalUnit)
                    .everyTimeValue(everyTimeValue)
                    .lastCheck(Timestamp.valueOf(LocalDateTime.now()))
                    .elementWrapper(elementWrapper)
                    .linkType(linkType)
                    .build();
        }

        siteSettingRepository.save(siteSettingEntity);
        schedulerManager.runThisSettingInScheduler(siteSettingEntity);

        bot.setLastCommandState(LastCommandState.WITHOUT);

        String settingReadyText = """
                Связка настроек запущена 🚀
                Ее можно увидеть в общем списке 📋
                """;
        bot.sendText(settingReadyText);
    }

    private String getElementWrapper(String url, String elUrl) {

        try {

            Document document = Jsoup.connect(url).get();
            Element element = document.selectFirst("a[href='" + elUrl + "']");

            if (element != null) {

                linkType = LinkType.ABSOLUTE;

                return findClassName(element);

            } else {

                linkType = LinkType.RELATIVE;

                elUrl = new URL(elUrl).getFile().replaceFirst("^/", "");
                element = document.selectFirst("a[href*='" + elUrl + "']");

                return findClassName(element);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String findClassName(Element element) {
        String className = element.attr("class");
        if (className.isEmpty()) {
            while (className.isEmpty()) {
                element = element.parent();
                className = element.attr("class");
            }
        }
        return className;
    }

    private boolean invalidUrl(String url) {
        return !url.startsWith("http://") && !url.startsWith("https://");
    }

    private TimeSettingOption getTimeSettingOption(String value) {

        if (!invalidTime(value))
            return TimeSettingOption.TIME_OF_DAY;
        else
            return TimeSettingOption.INTERVAL;

    }

    private IntervalUnit getIntervalUnit(CallbackQuery callbackQuery) {

        if (callbackQuery.getData().equalsIgnoreCase("m"))
            return IntervalUnit.MINUTE;
        else if (callbackQuery.getData().equalsIgnoreCase("h"))
            return IntervalUnit.HOUR;
        else
            return IntervalUnit.DAY;

    }

    private boolean invalidTime(String value) {
        try {
            LocalTime.parse(value);
            return false;
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private boolean invalidInteger(String value) {
        try {
            int number = Integer.parseInt(value);
            return number > 30;
        } catch (NumberFormatException e) {
            return true;
        }
    }

}