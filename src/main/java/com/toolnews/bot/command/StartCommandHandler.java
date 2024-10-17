package com.toolnews.bot.command;

import com.toolnews.bot.NewsBot;
import com.toolnews.bot.entity.enumeration.LastCommandState;
import org.springframework.stereotype.Service;

@Service
public class StartCommandHandler implements CommandHandler {

    public void handle(NewsBot bot) {

        String startCommandText = """
                Приветствую 🤝
                
                Ваш бот-сборщик новостей готов к работе 🤖
                
                Перед началом работы прочтите инструкцию 📋
                Ее можно получить по команде /help.
                """;

        bot.sendText(startCommandText);
        bot.setLastCommandState(LastCommandState.START);

    }

}
