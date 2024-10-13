package com.toolnews.bot.scheduler;

import com.toolnews.bot.entity.SiteSettingEntity;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.toolnews.bot.NewsBot.CHAT_ID;

@AllArgsConstructor
public class DefaultScheduler implements Scheduler {

    private SiteSettingEntity setting;
    private TelegramClient client;

    @Scheduled
    @Override
    public void run() {

        try {
            Document document = Jsoup.connect(setting.getListUrl()).get();
            String selector = setting.getElementWrapper()+"  ";

            String lastElementUrl = setting.getLastElementUrl();
            List<String> documentElements = document.getElementsByClass(selector).stream()
                    .map(el -> Objects.requireNonNull(
                            el.getElementsByTag("a").first()).attr("href"))
                    .toList();

            for (String link : documentElements) {

                if (link.equals(lastElementUrl)) {
                    return;
                } else {

                    SendMessage message = SendMessage.builder()
                            .text(link)
                            .chatId(CHAT_ID)
                            .build();

                    client.execute(message);

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
