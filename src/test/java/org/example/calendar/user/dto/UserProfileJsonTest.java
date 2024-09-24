package org.example.calendar.user.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.assertThat;

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class UserProfileJsonTest {
    @Autowired
    private JacksonTester<UserProfile> jacksonTester;
    @Autowired
    private JacksonTester<List<UserProfile>> listJacksonTester;
    private static final Faker FAKER = new Faker();

    @Test
    void shouldSerializeUserProfile() throws IOException {
        Long id = FAKER.number().numberBetween(1L, 100L);
        String name = FAKER.internet().username();
        UserProfile userProfile = new UserProfile(id, name);
        String json = String.format("""
                {
                    "id": %d,
                    "name": "%s"
                }
                """, id, name);

        JsonContent<UserProfile> actual = this.jacksonTester.write(userProfile);

        assertThat(actual).isEqualToJson(json);
    }

    @Test
    void shouldSerializeUserProfileList() throws IOException {
        Long id1 = FAKER.number().numberBetween(1L, 100L);
        String name1 = FAKER.internet().username();
        Long id2 = FAKER.number().numberBetween(1L, 100L);
        String name2 = FAKER.internet().username();
        List<UserProfile> profiles = List.of(new UserProfile(id1, name1), new UserProfile(id2, name2));

        // For more complex objects we can have a json file
        String json = String.format("""
                [
                    {
                        "id": %d,
                        "name": "%s"
                    },
                    {
                        "id": %d,
                        "name": "%s"

                    }
                ]
                """, id1, name1, id2, name2);

        JsonContent<List<UserProfile>> actual = this.listJacksonTester.write(profiles);

        assertThat(actual).isEqualToJson(json);
    }
}
