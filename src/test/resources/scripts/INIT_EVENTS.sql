INSERT INTO day_events (id, user_id, start_date, end_date, repetition_frequency, repetition_step,
                        repetition_duration, repetition_end_date, repetition_occurrences)
VALUES ('4472d36c-2051-40e3-a2cf-00c6497807b5', 2, '2024-09-30', '2024-09-30', 'DAILY', 4, 'UNTIL_DATE', '2024-10-14',
        NULL),
       ('6b9b32f2-3c2a-4420-9d52-781c09f320ce', 3, '2024-09-30', '2024-09-30', 'NEVER', NULL, NULL, NULL, NULL);

INSERT INTO day_event_slots (id, day_event_id, title, location, start_date, end_date)
VALUES ('5ff9cedf-ee36-4ec2-aa2e-5b6a16708ab0', '4472d36c-2051-40e3-a2cf-00c6497807b5', 'Event title', 'Location',
        '2024-09-30', '2024-09-30'),
       ('009d1441-ab86-411a-baeb-77a1d976868f', '4472d36c-2051-40e3-a2cf-00c6497807b5', 'Event title', 'Location',
        '2024-10-04', '2024-10-04'),
       ('35bdbe9f-9c5b-4907-8ae9-a983dacbda43', '4472d36c-2051-40e3-a2cf-00c6497807b5', 'Event title', 'Location',
        '2024-10-08', '2024-10-08'),
       ('e2985eda-5c5a-40a0-851e-6dc088081afa', '4472d36c-2051-40e3-a2cf-00c6497807b5', 'Event title', 'Location',
        '2024-10-12', '2024-10-12'),
       ('9c6f34b8-4128-42ec-beb1-99c35af8d7fa', '6b9b32f2-3c2a-4420-9d52-781c09f320ce', 'Event title', 'Location',
        '2024-10-29', '2024-10-30');

INSERT INTO day_event_slot_guest_emails
VALUES ('9c6f34b8-4128-42ec-beb1-99c35af8d7fa', 'ericka.ankunding@hotmail.com');

-- UTC times
INSERT INTO time_events (id, user_id, start_time, start_time_zone_id, end_time, end_time_zone_id,
                         repetition_frequency, repetition_step, monthly_repetition_type,
                         repetition_duration, repetition_end_date, repetition_occurrences)
VALUES ('0c9d6398-a6de-47f0-8328-04a2f3c0511c', 1, '2024-10-11T09:00:00', 'Europe/London', '2024-10-15T14:00:00',
        'Europe/London', 'WEEKLY', 2, NULL, 'N_REPETITIONS', NULL, 3),
       ('00026b7d-85ad-490a-8875-a85b3e5f37de', 2, '2024-10-28T13:00:00', 'Asia/Tokyo', '2024-10-28T15:30:00',
        'Asia/Tokyo', 'NEVER', NULL, NULL, NULL, NULL, NULL);

INSERT INTO time_event_slots (id, time_event_id, title, location, description, start_time, start_time_zone_id, end_time,
                              end_time_zone_id)
VALUES ('3075c6eb-8028-4f99-8c6c-27db1bb5cc43', '0c9d6398-a6de-47f0-8328-04a2f3c0511c', 'Event title', 'Location',
        'Description', '2024-10-11T09:00:00', 'Europe/London', '2024-10-15T14:00:00', 'Europe/London'), -- UTC times
       ('f8020ab5-1bc8-4b45-9d77-1a3859c264dd', '0c9d6398-a6de-47f0-8328-04a2f3c0511c', 'Event title', 'Location',
        'Description', '2024-10-25T09:00:00', 'Europe/London', '2024-10-29T14:00:00', 'Europe/London'), -- UTC times
       ('446d9d18-2a94-4bcf-b70d-b79941e9c31a', '0c9d6398-a6de-47f0-8328-04a2f3c0511c', 'Event title', 'Location',
        'Description', '2024-11-08T10:00:00', 'Europe/London', '2024-11-12T15:00:00', 'Europe/London'), -- UTC times after DST
       ('cdcf754a-8ebd-45aa-bd0c-85719e3b16a2', '0c9d6398-a6de-47f0-8328-04a2f3c0511c', 'Event title', 'Location',
        'Description', '2024-11-22T10:00:00', 'UTC', '2024-11-26T15:00:00', 'UTC'),
       ('77a7f7aa-a075-42fb-a06f-7d0fb5016352', '00026b7d-85ad-490a-8875-a85b3e5f37de', 'Event title', 'Location',
        'Description', '2024-10-28T13:00:00', 'Asia/Tokyo', '2024-10-28T15:30:00', 'Asia/Tokyo');

INSERT INTO time_event_slot_guest_emails
VALUES ('3075c6eb-8028-4f99-8c6c-27db1bb5cc43', 'ericka.ankunding@hotmail.com');
INSERT INTO time_event_slot_guest_emails
VALUES ('f8020ab5-1bc8-4b45-9d77-1a3859c264dd', 'ericka.ankunding@hotmail.com');