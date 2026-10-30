/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright Â© 2021-2026 Subhrodip Mohanta (hello@subho.xyz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package xyz.subho.clone.twitter.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

class ControllerExceptionHandlerTest {

  private final ControllerExceptionHandler handler = new ControllerExceptionHandler();
  private final WebRequest request = mock(WebRequest.class);

  @Test
  void malformedRequestsReturnBadRequestWithoutInternalDetails() {
    when(request.getDescription(false)).thenReturn("uri=/v1/posts");

    var response = handler.malformedRequestExceptionHandler(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).isEqualTo("Malformed request");
    assertThat(response.getBody().description()).isEqualTo("uri=/v1/posts");
  }

  @Test
  void duplicateDataReturnsConflictWithoutDatabaseDetails() {
    when(request.getDescription(false)).thenReturn("uri=/v1/users");

    var response = handler.dataIntegrityExceptionHandler(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().message()).isEqualTo("Request conflicts with existing data");
  }

  @Test
  void resourceNotFoundAndBadRequestsPreservePublicMessages() {
    when(request.getDescription(false)).thenReturn("uri=/v1/posts/1");

    var notFound =
        handler.resourceNotFoundExceptionHandler(new ResourceNotFoundException("missing"), request);
    var badRequest =
        handler.badRequestExceptionHandler(new BadRequestException("invalid"), request);

    assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(notFound.getBody().message()).isEqualTo("missing");
    assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(badRequest.getBody().message()).isEqualTo("invalid");
  }

  @Test
  void validationAndAccessErrorsHaveSafeMessages() {
    when(request.getDescription(false)).thenReturn("uri=/v1/users");
    BindingResult bindingResult = mock(BindingResult.class);
    MethodArgumentNotValidException validationException =
        mock(MethodArgumentNotValidException.class);
    when(validationException.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors())
        .thenReturn(List.of(new FieldError("request", "email", "must be valid")));

    var validation = handler.validationExceptionHandler(validationException, request);
    var accessDenied =
        handler.accessDeniedExceptionHandler(new AccessDeniedException("private"), request);

    assertThat(validation.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(validation.getBody().message()).isEqualTo("Validation failed: email must be valid");
    assertThat(accessDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(accessDenied.getBody().message()).isEqualTo("Access Denied: private");
  }

  @Test
  void unexpectedExceptionsAreLoggedAndSanitized() {
    when(request.getDescription(false)).thenReturn("uri=/v1/users");

    var response = handler.globalExceptionHandler(new IllegalStateException("secret"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().message()).isEqualTo("Internal server error");
    assertThat(response.getBody().message()).doesNotContain("secret");
  }

  @Test
  void allErrorConstructorsRetainTheirCauseAndMessage() {
    Throwable cause = new IllegalStateException("cause");

    assertThat(new BadRequestException().getMessage()).isNull();
    assertThat(new BadRequestException("message", cause).getCause()).isEqualTo(cause);
    assertThat(new BadRequestException("message").getMessage()).isEqualTo("message");
    assertThat(new BadRequestException(cause).getCause()).isEqualTo(cause);
    assertThat(new ResourceNotFoundException().getMessage()).isNull();
    assertThat(new ResourceNotFoundException("message", cause).getCause()).isEqualTo(cause);
    assertThat(new ResourceNotFoundException("message").getMessage()).isEqualTo("message");
    assertThat(new ResourceNotFoundException(cause).getCause()).isEqualTo(cause);
  }
}
