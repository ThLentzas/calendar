package org.example.google_calendar_clone.auth;

import org.example.google_calendar_clone.exception.UnauthorizedException;
import org.example.google_calendar_clone.role.RoleService;
import org.example.google_calendar_clone.user.UserService;
import org.example.google_calendar_clone.utils.CookieUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private JwtService jwtService;
    @Mock
    private UserService userService;
    @Mock
    private RoleService roleService;
    @InjectMocks
    private AuthService underTest;

    // refresh()
    @Test
    void shouldThrowUnauthorizedExceptionWhenRefreshTokenCookieIsNotFound() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        try (MockedStatic<CookieUtils> mockedStatic = Mockito.mockStatic(CookieUtils.class)) {
            mockedStatic.when(() -> CookieUtils.getCookie(any(HttpServletRequest.class), eq("REFRESH_TOKEN")))
                    .thenReturn(null);

            assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
                    this.underTest.refresh(servletRequest, servletResponse)).withMessage("Unauthorized");
        }
    }
}
