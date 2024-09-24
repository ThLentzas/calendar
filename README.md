
## Calendar System

A Calendar Event System for managing events, including day events, time events, and recurring events. It handles gracefully DST gaps/overlaps, leap years and time zone-aware times. It also supports a Contact/Friendship system where users can invite their contacts directly to events

## Features
- Support for both date and time events
- Event notification via email
- Event invitation via email
- Contact/Friendship system

## Technologies
* Java 17
* Spring Boot 3.3.2
* Flyway
* Thymeleaf
* Redis
* GreenMail
* Rest Assured

## API Endpoints

### Authentication
- `POST /auth/register` Register a user
- `POST /auth/login` Login a user
- `POST /auth/token/refresh` Refresh the access token
- `POST /auth/token/revoke` Revoke the access token
- `POST /auth/token/csrf` Return a csrf token

### User

- `POST /user/contacts` Send a contact request
- `GET /user/contact-requests` Return a list of pending contact requests
- `PUT /user/contact-requests` Accept/Reject a pending contact request
- `GET /user/contact-requests` Return a list of contacts

### Events

- `POST /events/day-events` Create a day event
- `PUT /events/day-events/{eventId}` Update a day event
- `GET /events/day-events/{eventId}` Return the schedule for a day event
- `DELETE /events/day-events/{eventId}` Delete a day event
- `POST /events/time-events` Create a time event
- `PUT /events/time-events/{eventId}` Update a time event
- `GET /events/time-events/{eventId}` Return the schedule for a time event
- `DELETE /events/time-events/{eventId}` Delete a time event
- `GET /events?startDate= &endDate=` Return a list of events that fall within the time range

### Event Slots

- `PUT /event-slots/day-event-slots/{slotId}/invite` Invite guests to a day event slot
- `PUT /event-slots/day-event-slots/{slotId}` Update a day event slot
- `GET /event-slots/day-event-slots/{slotId}` Return a day event slot
- `DELETE /event-slots/day-event-slots/{slotId}` Delete a day event slot
- `PUT /event-slots/time-event-slots/{slotId}/invite` Invite guests to a time event slot
- `PUT /event-slots/time-event-slots/{slotId}` Update a time event slot
- `GET /event-slots/time-event-slots/{slotId}` Return a time event slot
- `DELETE /event-slots/time-event-slots/{slotId}` Delete a time event slot

Events can repeat on various intervals—daily, weekly, monthly, or annually—and continue based on one of the following conditions:

    Until a specific date
    For a set number of occurrences
    Indefinitely (forever)

Additionally, events can repeat every n days, weeks, months, or years. Depending on the recurrence frequency, different settings apply:
Weekly Recurrence

For weekly recurring events, the system allows specifying which days of the week the event occurs. For example, an event might occur every Monday, Wednesday, and Friday.


For monthly recurring events, the system provides two options for how the event repeats:

    Same Day of the Month:
    The event can occur on the same calendar day each month.
    Example: An event that occurs on the 15th of the month.

    Same Weekday of the Month:
    The event can occur on a specific weekday within the month.
    Example: An event that occurs on the 4th Thursday of the month.

Handling Special Cases (5th Weekday)

In some months, certain weekdays may not occur five times. For example, not every month has a 5th Monday. In such cases, when an event is scheduled for the 5th occurrence of a specific weekday (e.g., the 5th Monday), the system automatically adjusts the event to occur on the last occurrence of that weekday in months where a 5th occurrence doesn’t exist

### Example for creating a Time Event:

```json
{
    "title": "Event title",
    "location": "Location",
    "description": "Description",
    "startTime": "2024-10-11T10:00",
    "endTime": "2024-10-15T15:00",
    "startTimeZoneId": "Europe/London",
    "endTimeZoneId": "Europe/London",
    "recurrenceFrequency": "WEEKLY",
    "recurrenceStep": 2,
    "recurrenceDuration": "N_OCCURRENCES",
    "numberOfOccurrences": 5
}
```

### Example for creating a Day Event:

```json
{
    "title": "Event title",
    "location": "Location",
    "description": "Description",
    "startDate": "2024-10-11",
    "endDate": "2024-10-15",
    "recurrenceFrequency": "WEEKLY",
    "recurrenceStep": 3,
    "weeklyRecurrenceDays": ["MONDAY", "FRIDAY"],
    "recurrenceDuration": "UNTIL_DATE",
    "recurrenceEndDate": "2025-01-12"
}
```