package com.toolnews.bot.command;

import com.toolnews.bot.NewsBot;
import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.LastCommandState;
import com.toolnews.bot.entity.enumeration.SettingState;
import com.toolnews.bot.entity.enumeration.TimeSettingOption;
import com.toolnews.bot.entity.enumeration.TimeSettingUnit;
import com.toolnews.bot.repository.SiteSettingRepository;
import com.toolnews.bot.scheduler.SchedulerManager;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class CreateSettingCommandHandler {

    private final SiteSettingRepository siteSettingRepository;

    private final SchedulerManager schedulerManager;

    private SettingState state = SettingState.CREATED;
    private String listUrl;
    private String lastElementUrl;
    private String elementWrapper;
    private TimeSettingOption timeSettingOption;
    private TimeSettingUnit timeSettingUnit;
    private Time newsCheckTime;
    private Integer everyTimeUnit;

    private SiteSettingEntity siteSettingEntity;

    public void resetState() {

        state = SettingState.CREATED;
        listUrl = null;
        lastElementUrl = null;
        elementWrapper = null;
        timeSettingOption = null;
        timeSettingUnit = null;
        newsCheckTime = null;
        everyTimeUnit = null;
        siteSettingEntity = null;

    }

    public void handle(NewsBot bot) {

        String createSettingCommandText = """
                Создаю связку настроек...
                """;
        bot.sendText(createSettingCommandText);

        String urlRequest = """
                Введите ссылку на страницу новостей.
                """;
        bot.sendText(urlRequest);

        state = SettingState.WAITING_LIST_URL;
        bot.setLastCommandState(LastCommandState.CREATE_SETTING);

    }


    @Transactional
    public void fillSiteSettings(NewsBot bot, String messageText) {

        if (state == SettingState.WAITING_LIST_URL) {

            if (invalidUrl(messageText)) {
                bot.sendText("""
                        Неверный формат ссылки.
                        
                        Ссылка должна начинаться на http:// или https://
                        
                        Попробуйте еще раз.
                        """);
                return;
            }

            listUrl = messageText;

            state = SettingState.WAITING_LAST_ELEMENT_URL;
            bot.sendText("Введите ссылку на одну из новостей.");

        } else if (state == SettingState.WAITING_LAST_ELEMENT_URL) {

            if (invalidUrl(messageText)) {
                bot.sendText("""
                        Неверный формат ссылки.
                        
                        Ссылка должна начинаться на http:// или https://
                        
                        Попробуйте еще раз.
                        """);
                return;
            }

            lastElementUrl = messageText;
            elementWrapper = getElementWrapper(listUrl, lastElementUrl);

            state = SettingState.WAITING_TIME;
            bot.sendText("Введите время или периодичность проверки.");

        } else if (state == SettingState.WAITING_TIME) {

            if (invalidTime(messageText) && invalidInteger(messageText)) {
                bot.sendText("""
                        Недопустимый формат. Введите время в 24-часовом формате, либо число не больше 30.
                        
                        Попробуйте еще раз.
                        """);
                return;
            }

            timeSettingOption = getTimeSettingOption(messageText);
            if (timeSettingOption == TimeSettingOption.TIME_OF_DAY) {

                newsCheckTime = Time.valueOf(LocalTime.parse(messageText));
                siteSettingEntity = SiteSettingEntity.builder()
                        .listUrl(listUrl)
                        .lastElementUrl(lastElementUrl)
                        .elementWrapper(elementWrapper)
                        .timeSettingOption(timeSettingOption)
                        .newsCheckTime(newsCheckTime)
                        .build();

                settingIsReady(bot);
                //______________________________________________________________________________

            } else {

                everyTimeUnit = Integer.parseInt(messageText);
                state = SettingState.WAITING_TIME_UNIT;

                bot.sendText("""
                        Введите значение периодичности.
                        """);

            }


        } else if (state == SettingState.WAITING_TIME_UNIT) {

            if (invalidTimeUnit(messageText)) {
                bot.sendText("""
                        Неверное значение. Можно вводить только Ч и Д без учета регистра.
                        """);
                return;
            }

            timeSettingUnit = getTimeSettingUnit(messageText);

            siteSettingEntity = SiteSettingEntity.builder()
                    .listUrl(listUrl)
                    .lastElementUrl(lastElementUrl)
                    .elementWrapper(elementWrapper)
                    .timeSettingOption(timeSettingOption)
                    .timeSettingUnit(timeSettingUnit)
                    .everyTimeUnit(everyTimeUnit)
                    .build();

            settingIsReady(bot);
            //______________________________________________________________________________

        }

    }


    @Transactional
    public void settingIsReady(NewsBot bot) {
        siteSettingRepository.save(siteSettingEntity);
        bot.setLastCommandState(LastCommandState.WITHOUT);
        schedulerManager.runThisSettingInScheduler(siteSettingEntity);
        bot.sendText("Связка настроек создана и готова к работе. Ее можно увидеть в общем списке.");
    }

    private String getElementWrapper(String url, String elUrl) {

        try {

            Document document = Jsoup.connect(url).get();
            Element element = document.selectFirst("a[href='" + elUrl + "']");

            while (true) {
                String className;
                try {
                    className = element.className();
                } catch (NullPointerException e) {
                    String relativeLink = getUriDifference(url, elUrl);
                    element = document.selectFirst("a[href*='" + relativeLink + "']");
                    lastElementUrl = element.attr("href");
                    continue;
                }
                if (className.isEmpty()) {
                    element = element.parent();
                    continue;
                }
                return className;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getUriDifference(String baseUrl, String fullUrl) throws MalformedURLException {

        String basePath = new URL(baseUrl).getPath();
        String fullPath = new URL(fullUrl).getPath();
        if (fullPath.startsWith(basePath)) {
            return fullPath.substring(basePath.length()).replaceFirst("^/", "");
        } else {
            return "";
        }

    }

    private Element getParent(Element element) {
        return element.parent();
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

    private TimeSettingUnit getTimeSettingUnit(String value) {

        if (value.equalsIgnoreCase("Ч"))
            return TimeSettingUnit.HOUR;
        else
            return TimeSettingUnit.DAY;

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

    private boolean invalidTimeUnit(String value) {
        return (!value.equalsIgnoreCase("ч")) && (!value.equalsIgnoreCase("д"));
    }

}