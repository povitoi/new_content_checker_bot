package com.news.content.checker.command;

import org.springframework.stereotype.Service;

@Service
public class StartCommandHandler implements CommandHandler {

    public String handle() {

        return """
                Приветствую 🤝
                
                Ваш бот-сборщик новостей готов к работе 🤖
                
                Перед началом прочтите инструкцию 📋
                Ее можно получить по команде /help.
                """;

    }

}
