package org.example.google_calendar_clone.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Year;
import java.time.format.TextStyle;

import org.example.google_calendar_clone.calendar.event.AbstractEventInvitationRequest;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventInvitationRequest;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventInvitationRequest;
import org.example.google_calendar_clone.exception.ServerErrorException;

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

    public static String formatFrequencyText(DayEventInvitationRequest emailRequest) {
        StringBuilder frequencyText = new StringBuilder();
        if (emailRequest.getRepetitionFrequency().equals(RepetitionFrequency.NEVER)) {
            return frequencyText.append(emailRequest.getStartDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .append(" ")
                    .append(emailRequest.getStartDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .append(" ")
                    .append(emailRequest.getStartDate().getDayOfMonth())
                    .append(", ")
                    .append(emailRequest.getStartDate().getYear())
                    .toString();
        }

        frequencyText.append(frequencyDescription(emailRequest, emailRequest.getStartDate()));
        if (!emailRequest.getRepetitionDuration().equals(RepetitionDuration.FOREVER)) {
            frequencyText.append(", ");
        }
        return frequencyText.append(dateDescription(emailRequest, emailRequest.getStartDate())).toString();
    }

    public static String formatFrequencyText(TimeEventInvitationRequest emailRequest) {
        LocalDateTime utcStartTime = DateUtils.convertToUTC(emailRequest.getStartTime(), emailRequest.getStartTimeZoneId());
        LocalDateTime utcEndTime = DateUtils.convertToUTC(emailRequest.getEndTime(), emailRequest.getEndTimeZoneId());
        String formatedTimeRange = DateUtils.formatTimeRange(utcStartTime, utcEndTime);
        StringBuilder frequencyText = new StringBuilder();

        if (emailRequest.getRepetitionFrequency().equals(RepetitionFrequency.NEVER)) {
            return frequencyText.append(utcStartTime.toLocalDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .append(" ")
                    .append(utcStartTime.toLocalDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .append(" ")
                    .append(utcStartTime.toLocalDate().getDayOfMonth())
                    .append(", ")
                    .append(utcStartTime.toLocalDate().getYear())
                    .append(" ")
                    .append(formatedTimeRange)
                    .toString();
        }

        frequencyText.append(frequencyDescription(emailRequest, emailRequest.getStartTime().toLocalDate()))
                .append(", ")
                .append(formatedTimeRange);
        if (!emailRequest.getRepetitionDuration().equals(RepetitionDuration.FOREVER)) {
            frequencyText.append(", ");
        }
        return frequencyText.append(dateDescription(emailRequest, emailRequest.getStartTime().toLocalDate()))
                .toString();
    }

    private static String frequencyDescription(AbstractEventInvitationRequest emailRequest, LocalDate startDate) {
        StringBuilder prefix = new StringBuilder();

        switch (emailRequest.getRepetitionFrequency()) {
            case DAILY -> prefix.append(emailRequest.getRepetitionStep() == 1 ? "Daily"
                    : String.format("Every %d days", emailRequest.getRepetitionStep()));
            case WEEKLY -> {
                String daysOfWeek = emailRequest.getWeeklyRecurrenceDays().stream()
                        .map(day -> day.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                        .collect(Collectors.joining(", "));
                prefix.append(emailRequest.getRepetitionStep() == 1 ? "Weekly, on " + daysOfWeek
                        : String.format("Every %d weeks on %s", emailRequest.getRepetitionStep(), daysOfWeek));
            }
            case MONTHLY -> {
                if (emailRequest.getMonthlyRepetitionType().equals(MonthlyRepetitionType.SAME_DAY)) {
                    prefix.append(emailRequest.getRepetitionStep() == 1 ? String.format("Monthly on day %d",
                            startDate.getDayOfMonth()) : String.format("Every %d months on day %d",
                            emailRequest.getRepetitionStep(), startDate.getDayOfMonth()));
                } else {
                    int occurrences = DateUtils.findDayOfMonthOccurrence(startDate);
                    boolean last = DateUtils.isLastOccurrenceOfMonth(startDate, occurrences);
                    prefix.append(emailRequest.getRepetitionStep() == 1 ? "Monthly on the " : String.format(
                            "Every %d months on the ", emailRequest.getRepetitionStep()));
                    prefix.append(!last ? ORDINAL_MAP.get(occurrences) : "last")
                            .append(" ").append(startDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                }
            }
            case ANNUALLY -> prefix.append(emailRequest.getRepetitionStep() == 1 ? "Annually on " : String.format(
                            "Every %d years on ", emailRequest.getRepetitionStep()))
                    .append(startDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                    .append(" ")
                    .append(startDate.getDayOfMonth());
            default -> throw new ServerErrorException("Internal Server error");
        }
        return prefix.toString();
    }

    // In the description, we include the start date and the repetition end date
    private static String dateDescription(AbstractEventInvitationRequest emailRequest, LocalDate startDate) {
        StringBuilder dateDescription = new StringBuilder();
        switch (emailRequest.getRepetitionDuration()) {
            case FOREVER -> {
                return dateDescription.toString();
            }
            case UNTIL_DATE -> {
                return dateDescription.append("from ")
                        .append(startDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                        .append(" ")
                        .append(startDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                        .append(" ")
                        .append(startDate.getDayOfMonth())
                        .append(Year.now().getValue() == startDate.getYear() ? " " : ", " + startDate.getYear() + " ")
                        .append("to ")
                        .append(emailRequest.getRepetitionEndDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                        .append(" ")
                        .append(emailRequest.getRepetitionEndDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                        .append(" ")
                        .append(Year.now().getValue() == emailRequest.getRepetitionEndDate().getYear()
                                ? emailRequest.getRepetitionEndDate().getDayOfMonth()
                                : emailRequest.getRepetitionEndDate().getDayOfMonth() + ", "
                                + emailRequest.getRepetitionEndDate().getYear())
                        .toString();
            }
            case N_REPETITIONS -> {
                return dateDescription.append(emailRequest.getRepetitionOccurrences())
                        .append(" times").toString();
            }
            default -> throw new ServerErrorException("Internal server error");
        }
    }
}
