package org.example.google_calendar_clone.utils;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventInvitationEmailRequest;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;

import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static String dayEventFrequencyText(DayEventInvitationEmailRequest emailRequest) {
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
                String prefix = emailRequest.getRepetitionStep() == 1 ? "Weekly, on " + daysOfWeek : "Every " +
                        emailRequest.getRepetitionStep() + " weeks on " + daysOfWeek;
                return frequencyDescription(emailRequest, prefix);
            }
            case MONTHLY -> {
                String prefix;
                if (emailRequest.getMonthlyRepetitionType().equals(MonthlyRepetitionType.SAME_DAY)) {
                    prefix = emailRequest.getRepetitionStep() == 1 ? "Monthly on day " +
                            emailRequest.getStartDate().getDayOfMonth() : "Every " + emailRequest.getRepetitionStep() +
                            " months on day " + emailRequest.getStartDate().getDayOfMonth() + ",";
                } else {
                    int occurrences = DateUtils.findDayOfMonthOccurrence(emailRequest.getStartDate());
                    boolean last = DateUtils.isLastOccurrenceOfMonth(emailRequest.getStartDate(), occurrences);
                    prefix = emailRequest.getRepetitionStep() == 1 ? "Monthly, on the " : " Every " +
                            emailRequest.getRepetitionStep() + " months on day " +
                            emailRequest.getStartDate().getDayOfMonth();
                    prefix = prefix + (!last ? ORDINAL_MAP.get(occurrences) : "last ") + " " +
                            emailRequest.getStartDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                }
                return frequencyDescription(emailRequest, prefix);
            }
            case ANNUALLY -> {
                String prefix = "Annually on " +
                        emailRequest.getStartDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " +
                        emailRequest.getStartDate().getDayOfMonth();
                return frequencyDescription(emailRequest, prefix);
            }
        }
        return null;
    }

    private static String frequencyDescription(DayEventInvitationEmailRequest emailRequest, String prefix) {
        if (emailRequest.getRepetitionEndDate() != null) {
            return prefix + "from " +
                    emailRequest.getStartDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                    emailRequest.getStartDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                    emailRequest.getStartDate().getDayOfMonth() + ", " +
                    emailRequest.getStartDate().getYear() + " to " +
                    emailRequest.getRepetitionEndDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                    emailRequest.getRepetitionEndDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " +
                    emailRequest.getRepetitionEndDate().getDayOfMonth() + ", " +
                    emailRequest.getRepetitionEndDate().getYear();
        }
        return prefix + ", " + emailRequest.getRepetitionOccurrences() + " times";
    }
}
