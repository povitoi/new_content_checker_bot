package com.toolnews.bot.command;

import com.toolnews.bot.NewsBot;
import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.repository.SiteSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.toolnews.bot.NewsBot.CHAT_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListOfSettingsCommandHandler implements CommandHandler {

    private final SiteSettingRepository siteSettingRepository;

    @Override
    public void handle(NewsBot bot) {

        List<SiteSettingEntity> settings = siteSettingRepository.findAll();

        if (settings.isEmpty()) {
            String listIsEmpty = """
                    На данный момент нет настроенных связок 🤔
                    """;
            bot.sendText(listIsEmpty);
            return;
        }

        String listOfSettingsCommandText = """
                Вывожу список настроек 📋
                """;

        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (SiteSettingEntity setting : settings) {

            try {
                String hostUrl = new URL(setting.getListUrl()).getHost();
                String settingId = setting.getId().toString();

                InlineKeyboardButton button = InlineKeyboardButton
                                .builder()
                                .text(hostUrl)
                                .callbackData(settingId)
                                .build();

               InlineKeyboardRow row = new InlineKeyboardRow();
               row.add(button);
               rows.add(row);

            } catch (MalformedURLException e) {
                log.error("An error occurred while trying to convert the link to a host. Stacktrace = {}"
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

    }

}
