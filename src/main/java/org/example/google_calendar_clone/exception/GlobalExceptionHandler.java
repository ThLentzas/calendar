package org.example.google_calendar_clone.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
class GlobalExceptionHandler {

    // @Valid exception
    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<ErrorMessage> handleMethodArgumentNotValidException(HttpServletRequest servletRequest,
                                                                               MethodArgumentNotValidException ma) {
        String message = ma.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErrorMessage errorMessage = new ErrorMessage(Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                ErrorMessage.ErrorType.BAD_REQUEST,
                message,
                servletRequest.getRequestURI()
        );

        return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    private ResponseEntity<ErrorMessage> handleDuplicateResourceException(HttpServletRequest servletRequest,
                                                                          DuplicateResourceException dre) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(),
                HttpStatus.CONFLICT.value(),
                ErrorMessage.ErrorType.CONFLICT,
                dre.getMessage(),
                servletRequest.getRequestURI()
        );

        return new ResponseEntity<>(errorMessage, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    private ResponseEntity<ErrorMessage> handleResourceNotFoundException(HttpServletRequest servletRequest,
                                                                         ResourceNotFoundException rnf) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                ErrorMessage.ErrorType.NOT_FOUND,
                rnf.getMessage(),
                servletRequest.getRequestURI()
        );

        return new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ContactRequestException.class)
    private ResponseEntity<ErrorMessage> handleContactRequestException(HttpServletRequest servletRequest,
                                                                              ContactRequestException cre) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(),
                HttpStatus.CONFLICT.value(),
                ErrorMessage.ErrorType.CONFLICT,
                cre.getMessage(),
                servletRequest.getRequestURI());

        return new ResponseEntity<>(errorMessage, HttpStatus.CONFLICT);
    }


    @ExceptionHandler(UnauthorizedException.class)
    private ResponseEntity<ErrorMessage> handleUnauthorizedException(HttpServletRequest servletRequest,
                                                                     UnauthorizedException ue) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                ErrorMessage.ErrorType.UNAUTHORIZED,
                ue.getMessage(),
                servletRequest.getRequestURI()
        );

        return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED);
    }
}
