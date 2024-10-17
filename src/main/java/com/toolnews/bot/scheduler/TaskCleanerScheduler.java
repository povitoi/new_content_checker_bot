package com.toolnews.bot.scheduler;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class TaskCleanerScheduler implements Scheduler {

    private ConcurrentHashMap<Long, ScheduledFuture<?>> runningSchedulers;

    public TaskCleanerScheduler(ConcurrentHashMap<Long, ScheduledFuture<?>> runningSchedulers) {
        this.runningSchedulers = runningSchedulers;
    }

    @Scheduled
    @Override
    public void run() {

        for(ScheduledFuture<?> scheduledFuture : runningSchedulers.values()) {
            if (scheduledFuture != null && scheduledFuture.isDone()) {
                scheduledFuture.cancel(false);
            }
        }

    }
}
