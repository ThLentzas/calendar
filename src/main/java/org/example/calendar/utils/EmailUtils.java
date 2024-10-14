package org.example.calendar.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Year;
import java.time.format.TextStyle;

import org.example.calendar.event.dto.AbstractEventInvitationRequest;
import org.example.calendar.event.day.dto.DayEventInvitationRequest;
import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.event.time.dto.TimeEventInvitationRequest;
import org.example.calendar.exception.ServerErrorException;

/*
    This class generates dynamically the frequency text for the invitation email and the date text for the notification
    email
 */
public final class EmailUtils {
    private static final Map<Integer, String> ORDINAL_MAP = new HashMap<>();

    static {
        ORDINAL_MAP.put(1, "first");
        ORDINAL_MAP.put(2, "second");
        ORDINAL_MAP.put(3, "third");
        ORDINAL_MAP.put(4, "fourth");
    }

    private EmailUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("EmailUtils is a utility class and cannot be instantiated");
    }

    /*
        Non-recurring event: "Thu Jan 9, 2025"

        Daily, until date: "Daily, from Thu Sep 12 to Sat Oct 12"
        Daily, n occurrences: "Daily, 10 times"
        Daily, forever: "Daily"

        Same logic applies for the remaining cases of DayEvents, and it also applies for TimeEvents
     */
    public static String buildFrequencyDescription(DayEventInvitationRequest emailRequest) {
        StringBuilder description = new StringBuilder();
        if (emailRequest.getRecurrenceFrequency().equals(RecurrenceFrequency.NEVER)) {
            return description.append(dateDescription(emailRequest.getStartDate(), TextStyle.SHORT, TextStyle.SHORT, true)).toString();
        }

        description.append(frequencyDescription(emailRequest, emailRequest.getStartDate()));
        if (!emailRequest.getRecurrenceDuration().equals(RecurrenceDuration.FOREVER)) {
            description.append(", ");
        }
        return description.append(dateDescription(emailRequest, emailRequest.getStartDate())).toString();
    }

    public static String buildFrequencyDescription(TimeEventInvitationRequest emailRequest) {
        LocalDateTime utcStartTime = DateUtils.convertToUTC(emailRequest.getStartTime(), emailRequest.getStartTimeZoneId());
        LocalDateTime utcEndTime = DateUtils.convertToUTC(emailRequest.getEndTime(), emailRequest.getEndTimeZoneId());
        String formatedTimeRange = DateUtils.formatTimeRange(utcStartTime, utcEndTime);
        StringBuilder description = new StringBuilder();

        if (emailRequest.getRecurrenceFrequency().equals(RecurrenceFrequency.NEVER)) {
            return description.append(dateDescription(emailRequest.getStartTime().toLocalDate(), TextStyle.SHORT, TextStyle.SHORT, true))
                    .append(" ")
                    .append(formatedTimeRange)
                    .toString();
        }

        description.append(frequencyDescription(emailRequest, emailRequest.getStartTime().toLocalDate()))
                .append(", ")
                .append(formatedTimeRange);
        if (!emailRequest.getRecurrenceDuration().equals(RecurrenceDuration.FOREVER)) {
            description.append(", ");
        }
        return description.append(dateDescription(emailRequest, emailRequest.getStartTime().toLocalDate()))
                .toString();
    }

    // "Thursday Sep 19, 2024"
    public static String buildDateDescription(LocalDate startDate) {
        return dateDescription(startDate, TextStyle.FULL, TextStyle.SHORT, true);
    }

    // "Wednesday Sep 18, 2024 2:30pm - 3:30pm"
    public static String buildDateTimeDescription(LocalDateTime startTime, LocalDateTime endTime) {
        String formatedTimeRange = DateUtils.formatTimeRange(startTime, endTime);

        return dateDescription(startTime.toLocalDate(), TextStyle.FULL, TextStyle.SHORT, true) + " " + formatedTimeRange;
    }

    private static String frequencyDescription(AbstractEventInvitationRequest emailRequest, LocalDate startDate) {
        StringBuilder prefix = new StringBuilder();

        switch (emailRequest.getRecurrenceFrequency()) {
            case DAILY ->
                    prefix.append(emailRequest.getRecurrenceStep() == 1 ? "Daily" : String.format("Every %d days", emailRequest.getRecurrenceStep()));
            case WEEKLY -> {
                String daysOfWeek = emailRequest.getWeeklyRecurrenceDays().stream()
                        .map(day -> day.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                        .collect(Collectors.joining(", "));
                prefix.append(emailRequest.getRecurrenceStep() == 1 ? "Weekly, on " + daysOfWeek : String.format("Every %d weeks on %s", emailRequest.getRecurrenceStep(), daysOfWeek));
            }
            case MONTHLY -> {
                if (emailRequest.getMonthlyRecurrenceType().equals(MonthlyRecurrenceType.SAME_DAY)) {
                    prefix.append(emailRequest.getRecurrenceStep() == 1 ? String.format("Monthly on day %d", startDate.getDayOfMonth()) : String.format("Every %d months on day %d", emailRequest.getRecurrenceStep(), startDate.getDayOfMonth()));
                } else {
                    int occurrences = DateUtils.findDayOfMonthOccurrence(startDate);
                    boolean last = DateUtils.isLastOccurrenceOfMonth(startDate, occurrences);
                    prefix.append(emailRequest.getRecurrenceStep() == 1 ? "Monthly on the " : String.format("Every %d months on the ", emailRequest.getRecurrenceStep()))
                            .append(!last ? ORDINAL_MAP.get(occurrences) : "last")
                            .append(" ")
                            .append(startDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                }
            }
            case ANNUALLY ->
                    prefix.append(emailRequest.getRecurrenceStep() == 1 ? "Annually on " : String.format("Every %d years on ", emailRequest.getRecurrenceStep()))
                            .append(startDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                            .append(" ")
                            .append(startDate.getDayOfMonth());
            default -> throw new ServerErrorException("Internal Server error");
        }
        return prefix.toString();
    }

    // In the description, we include the start date and the recurrence end date
    private static String dateDescription(AbstractEventInvitationRequest emailRequest, LocalDate startDate) {
        StringBuilder dateDescription = new StringBuilder();
        switch (emailRequest.getRecurrenceDuration()) {
            case FOREVER -> {
                return dateDescription.toString();
            }
            case UNTIL_DATE -> {
                boolean includeYear = Year.now().getValue() != startDate.getYear();
                dateDescription.append("from ")
                        .append(dateDescription(startDate, TextStyle.SHORT, TextStyle.SHORT, includeYear));
                includeYear = Year.now().getValue() != emailRequest.getRecurrenceEndDate().getYear();
                return dateDescription.append(" to ")
                        .append(dateDescription(emailRequest.getRecurrenceEndDate(), TextStyle.SHORT, TextStyle.SHORT, includeYear))
                        .toString();
            }
            case N_OCCURRENCES -> {
                return dateDescription.append(emailRequest.getNumbersOfOccurrences())
                        .append(" times")
                        .toString();
            }
            default -> throw new ServerErrorException("Internal server error");
        }
    }

    private static String dateDescription(LocalDate date, TextStyle dayTextStyle, TextStyle monthTextStyle, boolean includeYear) {
        StringBuilder dateDescription = new StringBuilder();
        dateDescription.append(date.getDayOfWeek().getDisplayName(dayTextStyle, Locale.ENGLISH))
                .append(" ")
                .append(date.getMonth().getDisplayName(monthTextStyle, Locale.ENGLISH))
                .append(" ");
        if (includeYear) {
            dateDescription.append(date.getDayOfMonth())
                    .append(", ")
                    .append(date.getYear());
        } else {
            dateDescription.append(date.getDayOfMonth());
        }

        return dateDescription.toString();
    }
}
