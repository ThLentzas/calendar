package org.example.google_calendar_clone.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;

public final class TestCookieUtils {
    private TestCookieUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("TestCookieUtils is a utility class and cannot be instantiated");
    }

    public static Map<String, String> parseCookies(HttpHeaders headers) {
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

            https://github.com/spring-projects/spring-security/issues/5673

            When the user is authenticated CsrfAuthenticationStrategy will replace the csrf token with a new one. This
            will happen in subsequent requests as well with the jwt, since the user is authenticated via the JwtAuthenticationProvider
            This is not true for sessions, since the authentication is loaded from Redis. We need to include this case
            in our logic, because Set-Cookie: XSRF-TOKEN=; Max-Age=0; Expires=Thu, 01-Jan-1970 00:00:10 GMT; Path=/;
            When we split XSRF-TOKEN= by "=" there will not be any value to access and the code below will fail with
            ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
                String[] cookieParts = header.split(";");
                String cookieName = cookieParts[0].split("=")[0];
                String cookieValue = cookieParts[0].split("=")[1];
         */
        if (cookies != null) {
            for (String cookie : cookies) {
                String[] cookieParts = cookie.split(";");
                String[] nameValuePair = cookieParts[0].split("=");
                if(nameValuePair.length == 2) {
                    String cookieName = cookieParts[0].split("=")[0];
                    String cookieValue = cookieParts[0].split("=")[1];
                    cookiesMap.put(cookieName, cookieValue);
                }
            }
        }

        return cookiesMap;
    }
}
