package com.toolnews.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class NewsBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final long ALLOWED_USER_ID = 701705313L;
    private static final long TARGET_GROUP_CHAT_ID = -4508940743L;

    private final TelegramClient client;

    public NewsBot() {
        client = new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public String getBotToken() {
        return "7414142924:AAFusHnSFZ71AzOY6I-82sYysFQdqh6uSXA";
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }

    private boolean isUserAllowed(Long userId) {
        return userId.equals(ALLOWED_USER_ID);
    }

    // -----------------------------------------------------------------------------------------------
    // UTILS START
    //
    //

    public void sendText(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //
    //
    // UTILS END

    // -----------------------------------------------------------------------------------------------

    // COMMANDS START
    //
    //

    private void handleStartCommand(Long chatId) {
        String startCommandText = """
                Для настройки бота используются следующие команды:
                /create_setting - создать связку настроек.
                /view_settings - посмотреть существующие.
                /delete_setting - удалить.
                """;
        sendText(chatId, startCommandText);
    }

    private void handleCreateSettingCommand(Long chatId) {

        String createSettingCommandText = """
                Для создания связки настроек для сайта необходимо выполнить следующие шаги:
                1. Перейти на сайт, на котором нужно проверять новости и зайти в раздел новостей.
                    Скопировать весь текст ссылки в адресной строке и отправить боту.
                2. Затем нажать на ссылку любой из новостей и также отправить ссылку боту.
                3. 
                """;

    }

    //
    //
    // COMMANDS END

    @Override
    public void consume(Update update) {

        if (update.hasMessage()) {

            Long chatId = update.getMessage().getChatId();

            if (isUserAllowed(chatId)) {

                String messageText = update.getMessage().getText();

                if (messageText.equals("/start")) {

                    handleStartCommand(chatId);

                } else if (messageText.equals("/create_setting")) {
                    handleCreateSettingCommand(chatId);
                }


            } else {
                sendText(chatId, "Вы не можете использовать этого бота");
            }

        }

    }

}