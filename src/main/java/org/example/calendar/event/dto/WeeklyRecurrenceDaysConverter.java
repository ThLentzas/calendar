package org.example.calendar.event.dto;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;

/*
    The autoApply = true flag in the @Converter annotation is used to indicate that the converter should be
    automatically applied to all entity attributes of the specified type (in this case, EnumSet<DayOfWeek>), without
    needing to explicitly declare the converter on each attribute. If we only wanted to be applied to that attribute
    of only a specific class we have to manually declare it.

    @Convert(converter = DayOfWeekEnumSetConverter.class)
    protected EnumSet<DayOfWeek> weeklyRecurrenceDays;

    @Converter(autoApply = true) did not work without specifying the converter

    A more efficient way to represent the EnumSet<DayOfWeek> weeklyRecurrenceDays is by using a single byte, where each
    day of the week is represented by a single bit. For example, the binary value 0b0010101 indicates that the event
    will occur on Monday, Wednesday, and Friday.
 */
public class WeeklyRecurrenceDaysConverter implements AttributeConverter<EnumSet<DayOfWeek>, String> {
    private static final String SEPARATOR = ",";

    // It can not be empty from the validation process
    @Override
    public String convertToDatabaseColumn(EnumSet<DayOfWeek> attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute.stream()
                .map(Enum::name)
                .collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public EnumSet<DayOfWeek> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }

        Set<DayOfWeek> days = Arrays.stream(dbData.split(SEPARATOR))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());

        return EnumSet.copyOf(days);
    }
}
