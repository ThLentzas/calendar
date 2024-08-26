package org.example.google_calendar_clone.user.contact.request.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.example.google_calendar_clone.user.contact.dto.CreateContactRequest;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class CreateContactRequestJsonTest {
    @Autowired
    private JacksonTester<CreateContactRequest> jacksonTester;
    private static final Faker FAKER = new Faker();

    @Test
    void shouldDeserializeCreateContactRequest() throws IOException {
        Long receiverId = FAKER.number().numberBetween(1L, 1000L);
        String json = String.format("""
                {
                    "receiverId": %d
                }
                """, receiverId);
        CreateContactRequest expected = new CreateContactRequest(receiverId);

        CreateContactRequest actual = this.jacksonTester.parseObject(json);

        assertThat(actual).isEqualTo(expected);
    }
}
