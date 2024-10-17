package com.toolnews.bot;

import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.IntervalUnit;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.*;

@Slf4j
public class BotUtils {

    public static void stopThread(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.error("An error occurred while trying to stop the thread. Stacktrace = {}",
                    e.getMessage());
        }
    }

    public static String getCronExpression(SiteSettingEntity setting) {
        LocalTime time = setting.getNewsCheckTime().toLocalTime();
        return "0 " + time.getMinute() + " " + time.getHour() + " * * *";
    }

    private static long convertIntervalToMinutes(IntervalUnit unit, int value) {
        if (unit == IntervalUnit.MINUTE) {
            return value;
        } else if (unit == IntervalUnit.HOUR)
            return value * 60;
        else
            return value * 60 * 24;
    }

    public static Instant getInstantForRunScheduler(
            Timestamp lastCheckTimestamp, IntervalUnit intervalUnit, int intervalValue) {

        LocalDateTime lastCheck = lastCheckTimestamp.toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();

        long elapsedTimeInMinutes = Duration.between(lastCheck, now).toMinutes();
        long durationInMinutes = convertIntervalToMinutes(intervalUnit, intervalValue);

        if (elapsedTimeInMinutes < durationInMinutes) {

            long remainingMinutes = durationInMinutes - elapsedTimeInMinutes;
            return now.plusMinutes(remainingMinutes)
                    .toInstant(ZoneOffset.of("+02:00"));

        } else {

            return Instant.now();

        }
    }

}
