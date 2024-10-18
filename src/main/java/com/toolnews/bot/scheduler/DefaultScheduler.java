package com.toolnews.bot.scheduler;

import com.toolnews.bot.BotUtils;
import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.LinkType;
import com.toolnews.bot.entity.enumeration.TimeSettingOption;
import com.toolnews.bot.repository.SiteSettingRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static com.toolnews.bot.NewsBot.CHAT_ID;


@Slf4j
public class DefaultScheduler implements Scheduler {

    private final SiteSettingRepository repository;

    private final SiteSettingEntity setting;
    private final TelegramClient client;
    private TaskScheduler taskScheduler = null;
    private ConcurrentHashMap<Long, ScheduledFuture<?>> runningSchedulers;

    public DefaultScheduler(
            SiteSettingEntity setting, TelegramClient client, SiteSettingRepository repository) {

        this.setting = setting;
        this.client = client;
        this.repository = repository;
    }

    public DefaultScheduler(
            SiteSettingEntity setting, TelegramClient client,
            SiteSettingRepository repository, TaskScheduler taskScheduler,
            ConcurrentHashMap<Long, ScheduledFuture<?>> runningSchedulers) {

        this.setting = setting;
        this.client = client;
        this.repository = repository;
        this.taskScheduler = taskScheduler;
        this.runningSchedulers = runningSchedulers;
    }

    @Scheduled
    @Override
    public void run() {

        String elementWrapper = setting.getElementWrapper();
        String listUrl = setting.getListUrl();
        String elementUrl = setting.getElementUrl();
        LinkType linkType = setting.getLinkType();

        try {
            Document document = Jsoup.connect(listUrl).get();

            List<String> documentElements = document.getElementsByClass(elementWrapper)
                    .stream()
                    .map(el -> {
                        if (el.is("a"))
                            return el.attr("href");
                        else
                            return el.getElementsByTag("a").first()
                                    .attr("href");
                    })
                    .toList();

            documentElements = getCleanList(documentElements, listUrl, elementUrl, linkType);

            if (documentElements.isEmpty()) {
                setting.setLastCheck(Timestamp.valueOf(LocalDateTime.now()));
                repository.save(setting);
                return;
            } else {

                URL hostUrl = new URL(listUrl);

                String lastElementUrl = new URL(hostUrl, documentElements.get(0)).toString();
                setting.setElementUrl(lastElementUrl);
                setting.setLastCheck(Timestamp.valueOf(LocalDateTime.now()));
                repository.save(setting);

                sendText("Появился новый материал на сайте \n" + hostUrl.getHost());

                BotUtils.stopThread(500);

                for (String link : documentElements) {
                    sendText(link);
                    BotUtils.stopThread(500);
                }

            }

        } catch (IOException e) {
            log.error("An error occurred while trying to retrieve a document from the network. Stacktrace = {}",
                    e.getMessage());
        } catch (NullPointerException e) {
            log.error("An error occurred while trying to get the value. Stacktrace = {}",
                    e.getMessage());
        }

        if (setting.getTimeSettingOption() == TimeSettingOption.INTERVAL) {

            BotUtils.stopThread(100);

            Instant instant = BotUtils.getInstantForRunScheduler(
                    setting.getLastCheck(),
                    setting.getIntervalUnit(),
                    setting.getEveryTimeValue());

            Scheduler scheduler = new DefaultScheduler(
                    setting, client, repository, taskScheduler, runningSchedulers);
            ScheduledFuture<?> task = taskScheduler.schedule(scheduler, instant);
            runningSchedulers.put(setting.getId(), task);

        }

    }

    private void sendText(String text) {

        SendMessage message = SendMessage.builder()
                .text(text)
                .chatId(CHAT_ID)
                .build();

        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            log.error("An error occurred while trying to send a SendMessage. Stacktrace = {}",
                    e.getMessage());
        }

    }

    private List<String> getCleanList(
            List<String> list, String hostUrl, String url, LinkType linkType
    ) throws MalformedURLException {

        String computableUrl = url;
        List<String> result = new ArrayList<>();

        if (linkType == LinkType.RELATIVE) {
            computableUrl = new URL(url).getFile();
            if (!list.get(0).startsWith("/"))
                computableUrl = computableUrl.replaceFirst("^/", "");
        }

        for (String link : list) {
            if (link.equals(computableUrl))
                return result;
            if (linkType == LinkType.RELATIVE) {
                String fullUrl = new URL(new URL(hostUrl), link).toString();
                result.add(fullUrl);
            } else
                result.add(link);
        }

        return result;
    }

}
