package com.toolnews.bot.command;

import com.toolnews.bot.BotUtils;
import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.CreateSettingState;
import com.toolnews.bot.entity.enumeration.IntervalUnit;
import com.toolnews.bot.entity.enumeration.LinkType;
import com.toolnews.bot.entity.enumeration.TimeSettingOption;
import com.toolnews.bot.repository.SiteSettingRepository;
import com.toolnews.bot.scheduler.SchedulerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateSettingCommandHandler implements CommandHandler {

    private final SiteSettingRepository siteSettingRepository;
    private final SchedulerManager schedulerManager;

    public static CreateSettingState state = CreateSettingState.CREATED;

    private String listUrl;
    private String elementUrl;
    private String elementWrapper;
    private LinkType linkType;
    private TimeSettingOption timeSettingOption;
    private IntervalUnit intervalUnit;
    private Time newsCheckTime;
    private Integer everyTimeValue;

    private SiteSettingEntity siteSettingEntity;


    public void resetStateSettingCreation() {

        state = CreateSettingState.CREATED;
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

    public String handle() {

        switch (state) {

            case CREATED -> {
                return """
                        Создаю связку настроек ⚙️
                        """;
            }
            case WAITING_LIST_URL -> {
                return """
                        1. Введите ссылку на список новостей 📝
                        """;
            }
            case WAITING_LAST_ELEMENT_URL -> {
                return """
                        2. Теперь ссылку на новейшую из них ✍️
                        """;
            }
            case WAITING_TIME -> {
                return """
                        3. Время или периодичность проверки 🕰
                        """;
            }
            case WAITING_TIME_UNIT -> {
                return """
                        4. Выберите значение периодичности 📆
                        """;
            }
            case READY -> {
                return """
                        Связка настроена и запущена 🚀
                        Ее можно увидеть в общем списке 📋
                        """;
            }

        }

        return "";

    }

    public InlineKeyboardMarkup getKeyboardForState() {

        if (state == CreateSettingState.WAITING_TIME_UNIT) {

            return InlineKeyboardMarkup
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
                    ).build();
        }

        return InlineKeyboardMarkup.builder().build();

    }

    public String fillListUrl(String url) {

        if (invalidUrl(url)) {
            return """
                    Неверный формат ссылки 🫢
                    
                    Ссылка должна начинаться на http:// или https://
                    
                    Попробуйте еще раз, это бесплатно 😌
                    """;
        }

        listUrl = url;
        return "";
    }

    public String fillLastElementUrl(String url) {

        if (invalidUrl(url)) {
            return """
                    Неверный формат ссылки 🤨
                    
                    Ссылка должна начинаться на http:// или https://
                    
                    Попробуйте еще раз, это бесплатно 🙂
                    """;
        }

        if (invalidUrlChain(url)) {
            return """
                    Вы ввели ссылки с разных сайтов 🤨
                    
                    Пожалуйста, введите корректные ссылки.
                    """;
        }

        elementUrl = url;
        elementWrapper = getElementWrapper(listUrl, elementUrl);

        if (elementWrapper == null || elementWrapper.isEmpty()) {
            return """
                    Во время подготовки настроек произошла программная ошибка. Пожалуйста, введите их заново, будьте внимательнее при копировании и вставке ссылок.
                    """;
        }

        return "";

    }

    public String fillTime(String time) {

        if (invalidTime(time) && invalidInteger(time)) {
            return """
                    Недопустимое значение 🧐
                    
                    Введите время в 24-часовом формате, либо число от 1 до 30
                    """;
        }

        timeSettingOption = getTimeSettingOption(time);

        if (timeSettingOption == TimeSettingOption.TIME_OF_DAY) {

            newsCheckTime = Time.valueOf(LocalTime.parse(time));
            return "0";

        } else {

            everyTimeValue = Integer.parseInt(time);
            return "1";

        }

    }

    public void fillTimeUnit(String unit) {

        intervalUnit = getIntervalUnit(unit);

    }

    @Transactional
    public void settingIsReady() {

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
            log.error("An error occurred while trying to retrieve a document from the network. Stacktrace = {}",
                    e.getMessage());
        } catch (NullPointerException e) {
            log.error("An error occurred while trying to retrieve the tag class. Stacktrace = {}",
                    e.getMessage());
            resetStateSettingCreation();
            return null;
        }
        return null;
    }

    private String findClassName(Element element) throws NullPointerException {
        String className = element.attr("class");
        if (className.isEmpty()) {
            while (className.isEmpty()) {
                if (element != null) {
                    element = element.parent();
                    if (element != null)
                        className = element.attr("class");
                }
            }
        }
        return className;
    }

    private boolean invalidUrl(String url) {
        return BotUtils.toUrl(url) == null;
    }

    private boolean invalidUrlChain(String url) {

        String listHost = BotUtils.toUrl(listUrl).getHost();
        String elementHost = BotUtils.toUrl(url).getHost();

        return !listHost.equals(elementHost);

    }

    private TimeSettingOption getTimeSettingOption(String value) {

        if (!invalidTime(value))
            return TimeSettingOption.TIME_OF_DAY;
        else
            return TimeSettingOption.INTERVAL;

    }

    private IntervalUnit getIntervalUnit(String callbackData) {

        if (callbackData.equalsIgnoreCase("m"))
            return IntervalUnit.MINUTE;
        else if (callbackData.equalsIgnoreCase("h"))
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
            return (number > 30) || (number < 1);
        } catch (NumberFormatException e) {
            return true;
        }
    }

}