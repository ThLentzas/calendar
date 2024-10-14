package org.example.calendar.exception;

import org.example.calendar.event.dto.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
class GlobalExceptionHandler {

    // @Valid/@Validated exception
    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<ErrorMessage> handleMethodArgumentNotValidException(HttpServletRequest servletRequest,
                                                                               MethodArgumentNotValidException ma) {
        // For field level errors we use .getFieldErrors()
        // For errors at a class level like a custom class validator we use .getGlobalErrors()
        // In our case we support both, so we use getAllErrors()
        String message = ma.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErrorMessage errorMessage = new ErrorMessage(Instant.now(), HttpStatus.BAD_REQUEST.value(), ErrorMessage.ErrorType.BAD_REQUEST, message, servletRequest.getRequestURI());
        return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    private ResponseEntity<ErrorMessage> handleDuplicateResourceException(HttpServletRequest servletRequest,
                                                                          DuplicateResourceException dre) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(), HttpStatus.CONFLICT.value(), ErrorMessage.ErrorType.CONFLICT, dre.getMessage(), servletRequest.getRequestURI());
        return new ResponseEntity<>(errorMessage, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    private ResponseEntity<ErrorMessage> handleResourceNotFoundException(HttpServletRequest servletRequest,
                                                                         ResourceNotFoundException rnf) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(), HttpStatus.NOT_FOUND.value(), ErrorMessage.ErrorType.NOT_FOUND, rnf.getMessage(), servletRequest.getRequestURI());
        return new ResponseEntity<>(errorMessage, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ServerErrorException.class)
    private ResponseEntity<ErrorMessage> handleServerErrorException(HttpServletRequest servletRequest,
                                                                    ServerErrorException se) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorMessage.ErrorType.INTERNAL_SERVER_ERROR, se.getMessage(), servletRequest.getRequestURI());
        return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ConflictException.class)
    private ResponseEntity<ErrorMessage> handleContactRequestException(HttpServletRequest servletRequest,
                                                                       ConflictException cre) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(), HttpStatus.CONFLICT.value(), ErrorMessage.ErrorType.CONFLICT, cre.getMessage(), servletRequest.getRequestURI());
        return new ResponseEntity<>(errorMessage, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnauthorizedException.class)
    private ResponseEntity<ErrorMessage> handleUnauthorizedException(HttpServletRequest servletRequest,
                                                                     UnauthorizedException ue) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(), HttpStatus.UNAUTHORIZED.value(), ErrorMessage.ErrorType.UNAUTHORIZED, ue.getMessage(), servletRequest.getRequestURI());
        return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED);
    }

    // It is thrown when the authenticated user is not owner of the resource
    @ExceptionHandler(AccessDeniedException.class)
    private ResponseEntity<ErrorMessage> handleAccessDeniedException(HttpServletRequest servletRequest,
                                                                     AccessDeniedException ade) {
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(), HttpStatus.FORBIDDEN.value(), ErrorMessage.ErrorType.FORBIDDEN, ade.getMessage(), servletRequest.getRequestURI());
        return new ResponseEntity<>(errorMessage, HttpStatus.FORBIDDEN);
    }
}
