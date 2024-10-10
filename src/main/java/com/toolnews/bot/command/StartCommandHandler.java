package com.toolnews.bot.command;

import com.toolnews.bot.NewsBot;
import com.toolnews.bot.entity.enumeration.LastCommandState;
import org.springframework.stereotype.Service;

@Service
public class StartCommandHandler {

    public void handle(NewsBot bot) {

        String startCommandText = """
                Приветствую, Навруз.
                Я твой бот для сбора новостей с разных сайтов.
                
                Для настройки моей работы используй команды из меню.
                
                Чтобы почитать инструкцию, выбери среди команд /help.
                """;

        bot.sendText(startCommandText);
        bot.setLastCommandState(LastCommandState.START);

    }

}
