package org.example.google_calendar_clone.user.contact.dto;

import org.example.google_calendar_clone.user.contact.ContactRequestStatus;
import org.example.google_calendar_clone.user.dto.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.io.IOException;
import java.util.List;

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.assertThat;

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class PendingContactRequestJsonTest {
    @Autowired
    private JacksonTester<List<PendingContactRequest>> jacksonTester;
    private static final Faker FAKER = new Faker();

    @Test
    void shouldSerializePendingContactRequestList() throws IOException {
        Long id1 = FAKER.number().numberBetween(1L, 1000L);
        String name1 = FAKER.internet().username();
        PendingContactRequest pendingContactRequest1 = new PendingContactRequest(
                new UserProfile(id1, name1),
                ContactRequestStatus.PENDING
        );
        Long id2 = FAKER.number().numberBetween(1L, 1000L);
        String name2 = FAKER.internet().username();
        PendingContactRequest pendingContactRequest2 = new PendingContactRequest(
                new UserProfile(id2, name2),
                ContactRequestStatus.PENDING
        );
        // For more complex objects we can have a json file
        String jsonRequests = String.format("""
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
        List<PendingContactRequest> requests = List.of(pendingContactRequest1, pendingContactRequest2);

        JsonContent<List<PendingContactRequest>> json = this.jacksonTester.write(requests);

        assertThat(json).isEqualToJson(jsonRequests);
    }
}
