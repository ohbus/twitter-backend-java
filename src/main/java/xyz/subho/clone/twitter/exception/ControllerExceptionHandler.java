/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright © 2021-2026 Subhrodip Mohanta (hello@subho.xyz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package xyz.subho.clone.twitter.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.Date;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import xyz.subho.clone.twitter.model.ErrorResponse;

@ControllerAdvice
public class ControllerExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ControllerExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> resourceNotFoundExceptionHandler(
      ResourceNotFoundException ex, WebRequest request) {
    var errorResponse =
        new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            new Date(),
            ex.getMessage(),
            request.getDescription(false));
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> badRequestExceptionHandler(
      BadRequestException ex, WebRequest request) {
    var errorResponse =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            new Date(),
            ex.getMessage(),
            request.getDescription(false));
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> validationExceptionHandler(
      MethodArgumentNotValidException ex, WebRequest request) {
    var errorResponse =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            new Date(),
            "Validation failed: "
                + ex.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .collect(Collectors.joining(", ")),
            request.getDescription(false));
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    ConstraintViolationException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ErrorResponse> malformedRequestExceptionHandler(WebRequest request) {
    return errorResponse(HttpStatus.BAD_REQUEST, "Malformed request", request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> dataIntegrityExceptionHandler(WebRequest request) {
    return errorResponse(HttpStatus.CONFLICT, "Request conflicts with existing data", request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> accessDeniedExceptionHandler(
      AccessDeniedException ex, WebRequest request) {
    var errorResponse =
        new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            new Date(),
            "Access Denied: " + ex.getMessage(),
            request.getDescription(false));
    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> globalExceptionHandler(
      Exception exception, WebRequest request) {
    log.error("Unhandled request failure", exception);
    return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
  }

  private ResponseEntity<ErrorResponse> errorResponse(
      HttpStatus status, String message, WebRequest request) {
    var errorResponse =
        new ErrorResponse(status.value(), new Date(), message, request.getDescription(false));
    return new ResponseEntity<>(errorResponse, status);
  }
}
