package org.example.google_calendar_clone.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ErrorMessageJsonTest {
    @Autowired
    private JacksonTester<ErrorMessage> jacksonTester;

    @Test
    void shouldSerializeErrorMessage() throws IOException {
        ErrorMessage errorMessage = new ErrorMessage(Instant.parse("2024-08-22T19:23:33.650400600Z"), 400, ErrorMessage.ErrorType.BAD_REQUEST, "The email field is required", "/api/v1/auth/register");
        String json = """
                {
                    "timestamp": "2024-08-22T19:23:33.650400600Z",
                    "status": 400,
                    "type": "BAD_REQUEST",
                    "message": "The email field is required",
                    "path": "/api/v1/auth/register"
                }
                """;

        JsonContent<ErrorMessage> actual = this.jacksonTester.write(errorMessage);

        assertThat(actual).isEqualToJson(json);
    }
}
