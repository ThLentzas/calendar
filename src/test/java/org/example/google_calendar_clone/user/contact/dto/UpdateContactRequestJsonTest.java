package org.example.google_calendar_clone.user.contact.dto;

import net.datafaker.Faker;
import org.example.google_calendar_clone.user.contact.ContactRequestAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class UpdateContactRequestJsonTest {
    @Autowired
    private JacksonTester<UpdateContactRequest> jacksonTester;
    private static final Faker FAKER = new Faker();

    @Test
    void shouldDeserializeUpdateContactRequest() throws IOException {
        Long senderId = FAKER.number().numberBetween(1L, 1000L);
        String json = String.format("""
                {
                    "senderId": %d,
                    "action": "ACCEPT"
                }
                """, senderId);

        UpdateContactRequest expected = new UpdateContactRequest(senderId, ContactRequestAction.ACCEPT);
        UpdateContactRequest actual = this.jacksonTester.parseObject(json);

        assertThat(actual).isEqualTo(expected);
    }
}
