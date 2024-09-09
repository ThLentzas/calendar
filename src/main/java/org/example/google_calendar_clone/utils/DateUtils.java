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
        We need to handle the following case:
        Events that repeat at the end of the month, where some months have 31 days other 30 and 28 or 29 for February
        if it is a leap year or not. For an event that is repeating at the same day and that is the last day of the
        month we have to adjust the upcoming events to fall on the last day of the month they are occurring. Below is
        the following example. We have an event that is to be repeated every month on the same day, at the 31st of
        January 2023 until the last day of June the 30th of the same year. The dates should be as follows
            "2023-01-31" => last day of January
            "2023-02-28" => last day of February for a non-leap year
            "2023-03-31" => last day of March
            "2023-04-30" => last day of April
            "2023-05-31" => last day of May
            "2023-06-30" => last day of June

        The day of the month passed is the day the event is to be repeated (the primitive value 1 - 31).
        In our case we have 31, and the date is the LocalDate of the month that we want our event to be repeated.
            In 1st calculation, it is 28 of February, we can not repeat the event at 31st of February, we need to move the event
            to the 28 or 29(leap year or not). We need to set the start date to 28 of February. For the given month,
            we get its length (30 - 31 or 28 - 29 for February). If the day of month > last day of month, it means we
            have a case like 31 January and 28 February. We move the start date to 28 of February.

            In 2nd calculation, if we simply moved to the 28 or 29 March, that is not the last day of March has 31 days.
            We need to adjust our start date. What is getting passed as date is 28 of March, we are passing the length
            of the month for March, it is 31. Now day of month (31) > last day of month(31) is false. It is the day
            of the initial request, the 31st of January and the 31st of March, its last day. In this case, we know there
            is the 31st day within the month so, we can just set the same day

            In 3rd calculation, same logic applies we check with April, 31 > 30, true move to the last day of the month
            30 of April

        This logic also applies correctly to adjust leap years. If we have an event that is repeating every year at
        the 29 of February for a leap year(2024), next year should be at February 28th for a non leap year(2025)

        We pass by copy and, we change where the copy reference points to by doing targetDate = targetDate.withDayOfMonth(lastDayOfMonth);
        so we need to return a value.
     */
    public static LocalDate adjustDateForMonth(int dayOfMonth, LocalDate date) {
        int lastDayOfMonth = date.lengthOfMonth();

        if (dayOfMonth > lastDayOfMonth) {
            // Adjust to the last valid day of the month if the desired day doesn't exist
            return date.withDayOfMonth(lastDayOfMonth);
        } else {
            // Otherwise, set the date to the original day of the month
            return date.withDayOfMonth(dayOfMonth);
        }
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
