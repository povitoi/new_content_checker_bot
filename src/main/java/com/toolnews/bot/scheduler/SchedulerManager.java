package com.toolnews.bot.scheduler;

import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.TimeSettingOption;
import com.toolnews.bot.entity.enumeration.TimeSettingUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

@Service
@RequiredArgsConstructor
public class SchedulerManager {

    private final TaskScheduler taskScheduler;
    private final ScheduledExecutorService executorService;
    private final ConcurrentHashMap<Long, Scheduler> runningSchedulers;
    private final TelegramClient client;

    public void runThisSettingInScheduler(SiteSettingEntity setting) {

        CronTrigger cron = new CronTrigger(getCronExpression(setting));

        Scheduler scheduler = new DefaultScheduler(setting, client);
        taskScheduler.schedule(scheduler, cron);
        runningSchedulers.put(setting.getId(), scheduler);

    }

    private String getCronExpression(SiteSettingEntity setting) {
        if (setting.getTimeSettingOption() == TimeSettingOption.TIME_OF_DAY) {
            LocalTime time = setting.getNewsCheckTime().toLocalTime();

            return "0 " + time.getMinute() + " " + time.getHour() + " * * *";
        } else {
            TimeSettingUnit timeUnit = setting.getTimeSettingUnit();
            int everyTime = setting.getEveryTimeUnit();
            if (timeUnit == TimeSettingUnit.HOUR) {
                return "0 */" + everyTime + " * * * *";
            } else {
                return "0 0 12 */" + everyTime + " * *";
            }

        }

    }

}
