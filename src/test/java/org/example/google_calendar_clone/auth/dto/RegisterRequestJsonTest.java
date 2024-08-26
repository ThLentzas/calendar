package org.example.google_calendar_clone.auth.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

import net.datafaker.Faker;

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class RegisterRequestJsonTest {
    @Autowired
    private JacksonTester<RegisterRequest> jacksonTester;
    private static final Faker FAKER = new Faker();

    @Test
    void shouldDeserializeRegisterRequest() throws IOException {
        String username = FAKER.internet().username();
        String email = FAKER.internet().emailAddress();
        String password = FAKER.internet().password(12, 128, true, true, true);
        String json = String.format("""
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """, username, email, password);
        RegisterRequest expected = new RegisterRequest(username, email, password);

        RegisterRequest actual = this.jacksonTester.parseObject(json);

        assertThat(actual).isEqualTo(expected);
    }
}
