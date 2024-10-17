package com.toolnews.bot.scheduler;

import com.toolnews.bot.BotUtils;
import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.TimeSettingOption;
import com.toolnews.bot.repository.SiteSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class SchedulerManager {

    private final TaskScheduler taskScheduler;
    private final SiteSettingRepository settingRepository;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> runningSchedulers;
    private final TelegramClient client;

//    @PostConstruct
//    public void runTaskCleanerScheduler() {
//
//        CronTrigger cron = new CronTrigger("0 0 12 * * *");
//        Scheduler scheduler = new TaskCleanerScheduler(runningSchedulers);
//        taskScheduler.schedule(scheduler, cron);
//
//    }

    public void runThisSettingInScheduler(SiteSettingEntity setting) {

        TimeSettingOption settingOption = setting.getTimeSettingOption();

        if (settingOption == TimeSettingOption.TIME_OF_DAY) {

            CronTrigger cron = new CronTrigger(BotUtils.getCronExpression(setting));
            Scheduler scheduler = new DefaultScheduler(setting, client, settingRepository);
            ScheduledFuture<?> task = taskScheduler.schedule(scheduler, cron);
            runningSchedulers.put(setting.getId(), task);

        } else {

            Instant instant = BotUtils.getInstantForRunScheduler(
                    setting.getLastCheck(),
                    setting.getIntervalUnit(),
                    setting.getEveryTimeValue());

            Scheduler scheduler = new DefaultScheduler(
                    setting, client, settingRepository, taskScheduler, runningSchedulers);
            ScheduledFuture<?> task = taskScheduler.schedule(scheduler, instant);
            runningSchedulers.put(setting.getId(), task);

        }

    }

}
