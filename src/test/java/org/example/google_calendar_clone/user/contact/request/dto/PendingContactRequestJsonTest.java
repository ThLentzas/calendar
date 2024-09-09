package org.example.google_calendar_clone.user.contact.request.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.example.google_calendar_clone.user.contact.dto.PendingContactRequest;
import org.example.google_calendar_clone.user.contact.request.ContactRequestStatus;
import org.example.google_calendar_clone.user.dto.UserProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.assertThat;

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class PendingContactRequestJsonTest {
    @Autowired
    private JacksonTester<List<PendingContactRequest>> listJacksonTester;
    private static final Faker FAKER = new Faker();

    @Test
    void shouldSerializePendingContactRequestList() throws IOException {
        Long id1 = FAKER.number().numberBetween(1L, 1000L);
        String name1 = FAKER.internet().username();
        Long id2 = FAKER.number().numberBetween(1L, 1000L);
        String name2 = FAKER.internet().username();
        List<PendingContactRequest> requests = List.of(new PendingContactRequest(new UserProfile(id1, name1),
                ContactRequestStatus.PENDING), new PendingContactRequest(new UserProfile(id2, name2),
                ContactRequestStatus.PENDING
        ));
        // For more complex objects we can have a json file
        String json = String.format("""
                [
                    {
                        "userProfile": {
                            "id": %d,
                            "name": "%s"
                        },
                        "status": PENDING
                    },
                    {
                        "userProfile": {
                            "id": %d,
                            "name": "%s"
                        },
                        "status": PENDING
                    }
                ]
                """, id1, name1, id2, name2);

        JsonContent<List<PendingContactRequest>> actual = this.listJacksonTester.write(requests);

        assertThat(actual).isEqualToJson(json);
    }
}
