/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright © 2021-2026 Subhrodip Mohanta (hello@subho.xyz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package xyz.subho.clone.twitter.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import xyz.subho.clone.twitter.entity.RefreshToken;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.model.AuthenticationRequest;
import xyz.subho.clone.twitter.model.TokenRefreshRequest;
import xyz.subho.clone.twitter.model.UserModel;
import xyz.subho.clone.twitter.security.JwtUtil;
import xyz.subho.clone.twitter.security.UserDetailsServiceImpl;
import xyz.subho.clone.twitter.service.RefreshTokenService;
import xyz.subho.clone.twitter.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtUtil jwtUtil;
  @Mock private UserDetailsServiceImpl userDetailsService;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private UserService userService;

  private AuthenticationController controller;
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    controller =
        new AuthenticationController(
            authenticationManager, jwtUtil, userDetailsService, refreshTokenService, userService);
  }

  @Test
  void loginAuthenticatesAndReturnsBothTokens() {
    AuthenticationRequest request = new AuthenticationRequest("alice", "password");
    UserDetails details = User.withUsername("alice").password("hash").build();
    UserModel model =
        new UserModel(
            userId, "alice", "Alice", null, "alice@example.com", null, null, 0L, 0L, false);
    RefreshToken refreshToken = token("refresh");
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);
    when(jwtUtil.generateToken(details)).thenReturn("jwt");
    when(userService.getUserByUserName("alice")).thenReturn(model);
    when(refreshTokenService.createRefreshToken(userId)).thenReturn(refreshToken);

    var response = controller.createAuthenticationToken(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().jwt()).isEqualTo("jwt");
    assertThat(response.getBody().refreshToken()).isEqualTo("refresh");
  }

  @Test
  void badCredentialsBecomeBadRequest() {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
    AuthenticationRequest request = new AuthenticationRequest("alice", "bad");

    assertThrows(BadRequestException.class, () -> controller.createAuthenticationToken(request));
  }

  @Test
  void loginRejectsAProfileRemovedAfterAuthentication() {
    when(userDetailsService.loadUserByUsername("alice"))
        .thenReturn(User.withUsername("alice").password("hash").build());
    when(userService.getUserByUserName("alice")).thenReturn(null);
    AuthenticationRequest request = new AuthenticationRequest("alice", "password");

    assertThrows(BadRequestException.class, () -> controller.createAuthenticationToken(request));
    verify(refreshTokenService, never()).createRefreshToken(any());
  }

  @Test
  void refreshReturnsArotatedToken() {
    RefreshToken replacement = token("new-refresh");
    replacement.getUsers().setUsername("alice");
    when(refreshTokenService.rotateRefreshToken("old-refresh")).thenReturn(replacement);
    when(userDetailsService.loadUserByUsername("alice"))
        .thenReturn(User.withUsername("alice").password("hash").build());
    when(jwtUtil.generateToken(any())).thenReturn("new-jwt");

    var response = controller.refreshToken(new TokenRefreshRequest("old-refresh"));

    assertThat(response.getBody().refreshToken()).isEqualTo("new-refresh");
    verify(refreshTokenService).rotateRefreshToken("old-refresh");
  }

  @Test
  void logoutDeletesAllRefreshTokensForThePrincipal() {
    Principal principal = () -> "alice";
    UserModel model =
        new UserModel(
            userId, "alice", "Alice", null, "alice@example.com", null, null, 0L, 0L, false);
    when(userService.getUserByUserName("alice")).thenReturn(model);

    assertThat(controller.logoutUser(principal).getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(refreshTokenService).deleteByUserId(userId);
  }

  @Test
  void logoutRejectsAStalePrincipal() {
    when(userService.getUserByUserName("alice")).thenReturn(null);

    assertThat(controller.logoutUser(() -> "alice").getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private RefreshToken token(String value) {
    RefreshToken token = new RefreshToken();
    token.setToken(value);
    token.setUsers(new Users());
    return token;
  }
}
