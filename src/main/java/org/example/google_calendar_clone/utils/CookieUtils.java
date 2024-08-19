package org.example.google_calendar_clone.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;

import java.time.Duration;

/*
    Read about cookies

    https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies
    https://datatracker.ietf.org/doc/html/draft-ietf-oauth-browser-based-apps-15#name-cookie-security
 */
public final class CookieUtils {
    private static final String ACCESS_TOKEN_NAME = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_NAME = "REFRESH_TOKEN";
    private static final String ACCESS_TOKEN_PATH = "/";
    private static final String REFRESH_TOKEN_PATH = "api/v1/auth/token/refresh";

    private CookieUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("CookieUtils is a utility class and cannot be instantiated");
    }

    // CookieCsrfTokenRepository
    public static Cookie generateCookie(String name, String value, String path, int duration) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value)
                // to use HTTPS only
                .secure(false)
                .path(path)
                .maxAge(Duration.ofMinutes(duration))
                // flag for HttpOnly Cookie
                .httpOnly(true)
                .sameSite("Lax");

        return mapToCookie(cookieBuilder.build());
    }

    // WebUtils from spring.framework
    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    public static void addAuthCookies(String accessTokenValue,
                                      int accessTokenDuration,
                                      String refreshTokenValue,
                                      int refreshTokenDuration,
                                      HttpServletResponse servletResponse) {
        Cookie accessTokenCookie = generateCookie(ACCESS_TOKEN_NAME, accessTokenValue, ACCESS_TOKEN_PATH, accessTokenDuration);
        Cookie refreshTokenCookie = generateCookie(REFRESH_TOKEN_NAME, refreshTokenValue, REFRESH_TOKEN_PATH, refreshTokenDuration);
        servletResponse.addCookie(accessTokenCookie);
        servletResponse.addCookie(refreshTokenCookie);
    }

    // CookieCsrfTokenRepository
    private static Cookie mapToCookie(ResponseCookie responseCookie) {
        Cookie cookie = new Cookie(responseCookie.getName(), responseCookie.getValue());
        cookie.setSecure(responseCookie.isSecure());
        cookie.setPath(responseCookie.getPath());
        cookie.setMaxAge((int) responseCookie.getMaxAge().getSeconds());
        cookie.setHttpOnly(responseCookie.isHttpOnly());

        if (StringUtils.hasLength(responseCookie.getDomain())) {
            cookie.setDomain(responseCookie.getDomain());
        }
        if (StringUtils.hasText(responseCookie.getSameSite())) {
            cookie.setAttribute("SameSite", responseCookie.getSameSite());
        }

        return cookie;
    }
}
