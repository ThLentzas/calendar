INSERT INTO users VALUES (1, 'joshua.wolf@hotmail.com', 'kris.hudson', '$2a$10$FHyVG3JuyVD6ZUMT5GsPa.oCCDgP0bcyP58/SAt8Tob7FeQHgrwsW');
INSERT INTO users VALUES (2, 'ericka.ankunding@hotmail.com', 'clement.gulgowski', '$2a$10$WYTJU2F0m8l5mxQUOSCqEOcZMpqvB/7ts19VJdssnhUPKXF.gS15e');
INSERT INTO users VALUES (3, 'waltraud.roberts@gmail.com', 'ellyn.roberts', '$2a$10$r9VAScmwEbpWAjCWHP04NeIpcW6vcKNBqJIw4QkMlxAqzGeTMNMfW');
INSERT INTO users VALUES (4, 'delois.abshire@hotmail.com', 'silas.stracke', '$2a$10$Xhex184CXDyvViZQUo/kVeRMlunXj2mTtSoEfTxTKmh9vmhiZhqXG');

/*
    When we manually insert specific id values (like 1, 2, 3) into a table with an auto-incrementing primary key (SERIAL),
    the sequence generator that handles the auto-incrementing does not automatically adjust to start at the next value
    after our manual insertions. If we do not adjust the sequence generator, we will have PK violations due to a duplicate
    key. When our code tries to insert a new user it will start from 1 and we will have a duplicate PK. We don't need
    to add + 1, SELECT setval('users_id_seq', (SELECT MAX(id) FROM users) + 1); because the generator will start from
    the next value of the current set one. In our case, it is set to 3, so it starts from 4. The default naming for
    the sequence generator is [table_name]_[column_name]_seq
 */
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
