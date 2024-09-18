package org.example.google_calendar_clone.calendar.event.dto;

import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class InviteGuestsRequestTest {
    @Autowired
    private JacksonTester<InviteGuestsRequest> jacksonTester;
    private static final Faker FAKER = new Faker();

    @Test
    void shouldDeserializeInviteGuestsRequest() throws IOException {
        String guestEmail = FAKER.internet().emailAddress();
        String json = String.format("""
                {
                    "guestEmails": ["%s"]
                }
                """, guestEmail);
        InviteGuestsRequest expected = new InviteGuestsRequest(Set.of(guestEmail));

        InviteGuestsRequest actual = this.jacksonTester.parseObject(json);

        assertThat(actual).isEqualTo(expected);
    }
}
