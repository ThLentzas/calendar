package org.example.google_calendar_clone.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

public final class CookieTestUtils {
    private CookieTestUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("CookieTestUtils is a utility class and cannot be instantiated");
    }

    public static Map<String, String> parseCookieHeaders(EntityExchangeResult<byte[]> response) {
        HttpHeaders headers = response.getResponseHeaders();
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        Map<String, String> cookiesMap = new HashMap<>();

        /*

            One of the cookies in the response Header(SET_COOKIE) is in the form of
            XSRF-TOKEN=546c0cc0-c895-4c9f-80df-06f589ce3378; Path=/

            We are using a double submit cookie pattern. The csrfCookie is XSRF-TOKEN=546c0cc0-c895-4c9f-80df-06f589ce3378
            which will be sent as a Cookie request header, and also we need to send an X-XSRF-TOKEN header with the value
            of the cookie. First we split by ";" that will give us "XSRF-TOKEN=546c0cc0-c895-4c9f-80df-06f589ce3378" as
            cookiesPart[0] then we can split again by "=" XSRF-TOKEN=546c0cc0-c895-4c9f-80df-06f589ce3378 gives us the value
            546c0cc0-c895-4c9f-80df-06f589ce3378. That is true for all Cookie name, value

         */
        if (cookies != null) {
            for (String header : cookies) {
                String[] cookieParts = header.split(";");
                String cookieName = cookieParts[0].split("=")[0];
                String cookieValue = cookieParts[0].split("=")[1];

                cookiesMap.put(cookieName, cookieValue);
            }
        }

        return cookiesMap;
    }
}
