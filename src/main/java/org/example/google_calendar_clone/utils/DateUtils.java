package org.example.google_calendar_clone.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.example.google_calendar_clone.exception.ServerErrorException;


public final class DateUtils {
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    private DateUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("DateUtils is a utility class and cannot be instantiated");
    }

    public static int findDayOfMonthOccurrence(LocalDate date) {
        // For the given year/month and index returns the day of the month
        LocalDate firstDayOfMonth = LocalDate.of(date.getYear(), date.getMonth(), 1);
        int occurrences = 0;
        for (LocalDate currentDate = firstDayOfMonth; !currentDate.isAfter(date); currentDate = currentDate.plusDays(1)) {
            if (currentDate.getDayOfWeek().equals(date.getDayOfWeek())) {
                occurrences++;
            }
        }
        return occurrences;
    }

    /*
        The date corresponds to the nth occurrence of a given day of the week within a month.
        2nd Tuesday of April 2024 -> returns the date 9/04/2024
     */
    public static LocalDate findDateOfNthDayOfWeekInMonth(YearMonth yearMonth, DayOfWeek day, int occurrences) {
        LocalDate firstDayOfMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), 1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        LocalDate date = null;

        for (LocalDate currentDate = firstDayOfMonth; !currentDate.isAfter(lastDayOfMonth); currentDate = currentDate.plusDays(1)) {
            if (currentDate.getDayOfWeek().equals(day)) {
                occurrences--;
            }
            if (occurrences == 0) {
                date = currentDate;
                break;
            }
        }
        if (date == null) {
            logger.info("The {} of {} in {} is null", occurrences, day, yearMonth);
            throw new ServerErrorException("Internal Server Error");
        }
        return date;
    }

    /*
        https://stackoverflow.com/questions/76191242/converting-utc-to-local-time-with-daylight-saving-in-java

        ZoneDateTime handles the DST we don't have to worry about it.
     */
    public static LocalDateTime convertToUTC(LocalDateTime dateTime, ZoneId zoneId) {
        // Convert the LocalDateTime to ZonedDateTime with the provided ZoneId
        ZonedDateTime zonedDateTime = dateTime.atZone(zoneId);

        // Convert the ZonedDateTime to UTC
        return zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }

    public static LocalDateTime convertFromUTC(LocalDateTime dateTime, ZoneId zoneId) {
        // Convert the LocalDateTime from UTC to ZonedDateTime in UTC
        ZonedDateTime utc = dateTime.atZone(ZoneId.of("UTC"));

        // Convert the ZonedDateTime to the specified time zone
        return utc.withZoneSameInstant(zoneId).toLocalDateTime();
    }
}
