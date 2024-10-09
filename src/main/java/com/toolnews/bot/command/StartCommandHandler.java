package com.toolnews.bot.command;

public class StartCommandHandler implements CommandHandler {
    @Override
    public String handle() {

        return """
                Для настройки бота используются следующие команды:
                /create_setting - создать связку настроек.
                /view_settings - посмотреть существующие.
                /delete_setting - удалить.
                """;

    }
}
