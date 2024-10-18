package com.toolnews.bot.command;

import com.toolnews.bot.NewsBot;
import org.springframework.stereotype.Service;

@Service
public class HelpCommandHandler implements CommandHandler {

    public void handle(NewsBot bot) {

        String createSettingCommandText = """
                /create_setting - создает новую связку настроек.
                
                Для этого необходимо ввести следующие данные поэтапно:
                
                1. Ссылка на страницу списка новостей.
                
                2. Ссылка на новость, которая будет считаться точкой отсчета. Начиная с нее бот будет проверять появление новостей.
                
                3. Время или число не больше 30. Если ввести время, например, 15:00, бот будет проверять новости раз в сутки в это время. Если указано число, то на следующем этапе будет предложено выбрать единицу периодичности.
                
                4. Если на предыдущем этапе введено число, то здесь нужно выбрать "Час" или "День". Если было введено число 5 и выбран "День", бот будет проверять новости каждые 5 дней.
                
                /list_of_settings - показывает список всех существующих связок.
                
                На каждую можно нажать, чтобы настроить или удалить.
                
                /help - показывает эту инструкцию.
                """;

        bot.sendText(createSettingCommandText);

    }

}
