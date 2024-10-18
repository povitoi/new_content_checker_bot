package com.toolnews.bot;

import com.toolnews.bot.entity.SiteSettingEntity;
import com.toolnews.bot.entity.enumeration.IntervalUnit;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.toolnews.bot.NewsBot.zoneOffset;
import static com.toolnews.bot.NewsBot.zonedId;

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

    public static String formatDate(LocalDate date) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return date.format(dateFormatter);
    }

    public static String formatTime(LocalTime time) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(timeFormatter);
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

    public static Map<String, String> getTimeDurationUntilNextCheck(
            LocalDateTime lastCheck, LocalTime checkTime) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextCheckTime = checkTime.atDate(LocalDate.now());
        if (nextCheckTime.isBefore(now))
            nextCheckTime = nextCheckTime.plusDays(1);

        Duration duration = Duration.between(now, nextCheckTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        Map<String, String> result = new HashMap<>();
        result.put("hours", String.valueOf(hours));
        result.put("minutes", String.valueOf(minutes));

        return result;
    }

    public static String prepareStringForIntervalOption(IntervalUnit unit, int value) {

        int valueLastSymbol = value % 10;
        int valueLastTwoSymbols = value % 100;
        StringBuilder sb = new StringBuilder();

        if (valueLastSymbol == 1 && valueLastTwoSymbols == 1) {

            String every = null;
            String timeUnit = null;
            if (unit == IntervalUnit.MINUTE) {
                every = "Каждую ";
                timeUnit = "минуту";
            } else if (unit == IntervalUnit.HOUR) {
                every = "Каждый ";
                timeUnit = "час";
            } else if (unit == IntervalUnit.DAY) {
                every = "Каждый ";
                timeUnit = "день";
            }

            sb.append(every);
            sb.append(" ");
            sb.append(timeUnit);

        } else if (valueLastSymbol >= 2 && valueLastSymbol <= 4) {

            String timeUnit = null;
            if (unit == IntervalUnit.MINUTE) {
                timeUnit = "минуты";
            } else if (unit == IntervalUnit.HOUR) {
                timeUnit = "часа";
            } else if (unit == IntervalUnit.DAY) {
                timeUnit = "дня";
            }

            sb.append("Каждые ");
            sb.append(value);
            sb.append(" ");
            sb.append(timeUnit);

        } else if (valueLastSymbol >= 5 || valueLastTwoSymbols == 10 || valueLastTwoSymbols == 11) {

            String timeUnit = null;
            if (unit == IntervalUnit.MINUTE) {
                timeUnit = "минут";
            } else if (unit == IntervalUnit.HOUR) {
                timeUnit = "часов";
            } else if (unit == IntervalUnit.DAY) {
                timeUnit = "дней";
            }

            sb.append("Каждые ");
            sb.append(value);
            sb.append(" ");
            sb.append(timeUnit);

        }

        return sb.toString();
    }

    public static String prepareStringForNextCheck(
            LocalDateTime lastCheck, IntervalUnit intervalUnit, int intervalValue
    ) {

        Instant nextCheckInstant = getInstantForRunScheduler(
                Timestamp.valueOf(lastCheck), intervalUnit, intervalValue);
        Instant nowInstant = Instant.now();
        Duration duration = Duration.between(nowInstant, nextCheckInstant);

        LocalDateTime nextCheck = nextCheckInstant.atZone(ZoneId.of(zonedId)).toLocalDateTime();
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        LocalDate tomorrow = nowDate.plusDays(1);

        String formattedNextCheckTime = formatTime(
                nowTime.plusHours(duration.toHours())
                        .plusMinutes(duration.toMinutes() % 60)
        );

        String formattedNextCheckDate = formatDate(
                nowDate.plusDays(duration.toDays())
        );

        StringBuilder sb = new StringBuilder();
        if (nextCheck.toLocalDate().equals(tomorrow)) {

            sb.append("Завтра в ");
            sb.append(formattedNextCheckTime);

        } else {

            sb.append(formattedNextCheckDate);
            sb.append(" в ");
            sb.append(formattedNextCheckTime);

        }

        return sb.toString();
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
                    .toInstant(ZoneOffset.of(zoneOffset));

        } else {

            return Instant.now();

        }
    }

}
