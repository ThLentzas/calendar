package org.example.google_calendar_clone.utils;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventInvitationEmailRequest;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventInvitationEmailRequest;

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

    public static String formatFrequencyDescription(DayEventInvitationEmailRequest emailRequest) {
        switch (emailRequest.getRepetitionFrequency()) {
            case NEVER -> {
                return emailRequest.getStartDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                        emailRequest.getStartDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                        emailRequest.getStartDate().getDayOfMonth() + ", " +
                        emailRequest.getStartDate().getYear();
            }
            case DAILY -> {
                String prefix = emailRequest.getRepetitionStep() == 1 ? "Daily, " : "Every " +
                        emailRequest.getRepetitionStep() + " days, ";
                return frequencyDescription(emailRequest, prefix);
            }
            case WEEKLY -> {
                String daysOfWeek = emailRequest.getWeeklyRecurrenceDays().stream()
                        .map(day -> day.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                        .collect(Collectors.joining(", "));
                String prefix = emailRequest.getRepetitionStep() == 1 ? "Weekly on " + daysOfWeek + " " : "Every " +
                        emailRequest.getRepetitionStep() + " weeks on " + daysOfWeek + " ";
                return frequencyDescription(emailRequest, prefix);
            }
            case MONTHLY -> {
                String prefix;
                if (emailRequest.getMonthlyRepetitionType().equals(MonthlyRepetitionType.SAME_DAY)) {
                    prefix = emailRequest.getRepetitionStep() == 1 ? "Monthly on day " +
                            emailRequest.getStartDate().getDayOfMonth() + ", " : "Every " + emailRequest.getRepetitionStep() +
                            " months on day " + emailRequest.getStartDate().getDayOfMonth() + ", ";
                } else {
                    int occurrences = DateUtils.findDayOfMonthOccurrence(emailRequest.getStartDate());
                    boolean last = DateUtils.isLastOccurrenceOfMonth(emailRequest.getStartDate(), occurrences);
                    prefix = emailRequest.getRepetitionStep() == 1 ? "Monthly on the " : "Every " +
                            emailRequest.getRepetitionStep() + " months on the ";
                    prefix = prefix + (last ? ORDINAL_MAP.get(occurrences) : "last ") + " " +
                            emailRequest.getStartDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", ";
                }
                return frequencyDescription(emailRequest, prefix);
            }
            case ANNUALLY -> {
                String prefix = (emailRequest.getRepetitionStep() == 1 ? "Annually on " : "Every " +
                        emailRequest.getRepetitionStep() + " years on " )+
                        emailRequest.getStartDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " +
                        emailRequest.getStartDate().getDayOfMonth() + ", ";
                return frequencyDescription(emailRequest, prefix);
            }
        }
        return null;
    }

    public static String formatFrequencyDescription(TimeEventInvitationEmailRequest emailRequest) {
        LocalDateTime utcStartTime = DateUtils.convertToUTC(emailRequest.getStartTime(), emailRequest.getStartTimeZoneId());
        LocalDateTime utcEndTime = DateUtils.convertToUTC(emailRequest.getEndTime(), emailRequest.getEndTimeZoneId());

        switch (emailRequest.getRepetitionFrequency()) {
            case NEVER -> {
                return emailRequest.getStartTime().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                        emailRequest.getStartTime().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                        emailRequest.getStartTime().getDayOfMonth() + ", " +
                        emailRequest.getStartTime().getYear() + " " ;
            }
        }
        return null;
    }

    private static String frequencyDescription(DayEventInvitationEmailRequest emailRequest, String prefix) {
        if (emailRequest.getRepetitionEndDate() != null) {
            return prefix + "from " +
                    emailRequest.getStartDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                    emailRequest.getStartDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                    emailRequest.getStartDate().getDayOfMonth() +
                    (Year.now().getValue() == emailRequest.getStartDate().getYear() ? " " : ", " +
                            emailRequest.getStartDate().getYear() + " " ) + "to " +
                    emailRequest.getRepetitionEndDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                    emailRequest.getRepetitionEndDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                    (Year.now().getValue() == emailRequest.getRepetitionEndDate().getYear()
                            ? emailRequest.getRepetitionEndDate().getDayOfMonth()
                            : emailRequest.getRepetitionEndDate().getDayOfMonth() + ", " +
                    emailRequest.getRepetitionEndDate().getYear());
        }
        return prefix + emailRequest.getRepetitionOccurrences() + " times";
    }
}
