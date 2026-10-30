/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright © 2021-2026 Subhrodip Mohanta (hello@subho.xyz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package xyz.subho.clone.twitter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.subho.clone.twitter.entity.RefreshToken;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.exception.ResourceNotFoundException;
import xyz.subho.clone.twitter.repository.RefreshTokenRepository;
import xyz.subho.clone.twitter.repository.UsersRepository;
import xyz.subho.clone.twitter.service.impl.RefreshTokenServiceImpl;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private UsersRepository usersRepository;

  private RefreshTokenServiceImpl service;
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new RefreshTokenServiceImpl(refreshTokenRepository, usersRepository);
    ReflectionTestUtils.setField(service, "refreshTokenDurationMs", 86_400_000L);
  }

  @Test
  void createReplacesTheUserTokenUnderTheUserLock() {
    Users user = user();
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefreshToken result = service.createRefreshToken(userId);

    assertThat(result.getUsers()).isEqualTo(user);
    assertThat(result.getToken()).isNotBlank();
    assertThat(ReflectionTestUtils.getField(result, "token")).isNotEqualTo(result.getToken());
    assertThat(result.getExpiryDate()).isAfter(Instant.now());
    verify(refreshTokenRepository).deleteByUsers(user);
    verify(refreshTokenRepository).flush();
  }

  @Test
  void creatingForAnUnknownUserIsRejected() {
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.createRefreshToken(userId));
  }

  @Test
  void expiredTokensAreDeletedAndRejected() {
    RefreshToken token = token(Instant.now().minusSeconds(1));
    when(refreshTokenRepository.findByTokenForUpdate(any())).thenReturn(Optional.of(token));

    assertThrows(BadRequestException.class, () -> service.rotateRefreshToken("old"));
    verify(refreshTokenRepository).delete(token);
  }

  @Test
  void validTokensRotateAndInvalidateTheOldValue() {
    Users user = user();
    RefreshToken current = token(Instant.now().plusSeconds(60));
    current.setUsers(user);
    when(refreshTokenRepository.findByTokenForUpdate(any())).thenReturn(Optional.of(current));
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefreshToken replacement = service.rotateRefreshToken("old");

    assertThat(replacement.getToken()).isNotEqualTo(current.getToken());
    verify(refreshTokenRepository, atLeastOnce()).deleteByUsers(user);
    verify(refreshTokenRepository, atLeastOnce()).flush();
  }

  @Test
  void rotationRejectsTokensForDeletedUsers() {
    Users user = user();
    RefreshToken current = token(Instant.now().plusSeconds(60));
    current.setUsers(user);
    when(refreshTokenRepository.findByTokenForUpdate(any())).thenReturn(Optional.of(current));
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.rotateRefreshToken("old"));
  }

  @Test
  void unknownTokensAreRejected() {
    when(refreshTokenRepository.findByTokenForUpdate(any())).thenReturn(Optional.empty());

    assertThrows(BadRequestException.class, () -> service.rotateRefreshToken("missing"));
  }

  @Test
  void rotationLooksUpOnlyTheHashOfThePresentedToken() {
    when(refreshTokenRepository.findByTokenForUpdate(any())).thenReturn(Optional.empty());

    assertThrows(BadRequestException.class, () -> service.rotateRefreshToken("presented"));

    verify(refreshTokenRepository).findByTokenForUpdate(argThat(value -> value.length() == 64));
  }

  @Test
  void nonPositiveRefreshDurationFailsFast() {
    ReflectionTestUtils.setField(service, "refreshTokenDurationMs", 0L);

    assertThrows(
        IllegalStateException.class,
        () -> ReflectionTestUtils.invokeMethod(service, "validateConfiguration"));
  }

  @Test
  void missingRefreshDurationFailsFast() {
    ReflectionTestUtils.setField(service, "refreshTokenDurationMs", null);

    assertThrows(
        IllegalStateException.class,
        () -> ReflectionTestUtils.invokeMethod(service, "validateConfiguration"));
  }

  @Test
  void logoutDelegatesToRepository() {
    Users user = user();
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.deleteByUsers(user)).thenReturn(1);

    assertThat(service.deleteByUserId(userId)).isEqualTo(1);
  }

  @Test
  void logoutForAnUnknownUserIsANoOp() {
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

    assertThat(service.deleteByUserId(userId)).isZero();
  }

  private Users user() {
    Users user = new Users();
    user.setId(userId);
    user.setUsername("alice");
    return user;
  }

  private RefreshToken token(Instant expiry) {
    RefreshToken token = new RefreshToken();
    token.setToken("old");
    token.setExpiryDate(expiry);
    return token;
  }
}
