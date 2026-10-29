/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright © 2021-2026 Subhrodip Mohanta (hello@subho.xyz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package xyz.subho.clone.twitter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class JwtSecurityTest {

  private final UserDetails user =
      User.withUsername("alice").password("hash").roles("USER").build();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void jwtRoundTripValidatesSubjectAndExpiry() {
    JwtUtil jwtUtil = jwtUtil(60_000L);
    String token = jwtUtil.generateToken(user);

    assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
    assertThat(jwtUtil.extractExpiration(token)).isAfter(Instant.now());
    assertThat(jwtUtil.validateToken(token, user)).isTrue();
    assertThat(jwtUtil.validateToken(token, User.withUsername("bob").password("hash").build()))
        .isFalse();
  }

  @Test
  void expiredAndMalformedTokensAreRejected() {
    JwtUtil expiredUtil = jwtUtil(-1L);
    String expired = expiredUtil.generateToken(user);
    assertThat(expiredUtil.validateToken(expired, user)).isFalse();

    JwtUtil jwtUtil = jwtUtil(60_000L);
    assertThrows(JwtException.class, () -> jwtUtil.extractUsername("not-a-jwt"));
    assertThat(jwtUtil.validateToken("not-a-jwt", user)).isFalse();
  }

  @Test
  void tokenWithoutASubjectIsRejectedWithoutThrowing() {
    JwtUtil jwtUtil = jwtUtil(60_000L);
    String token =
        Jwts.builder()
            .expiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(
                Keys.hmacShaKeyFor(
                    "test-only-secret-that-is-at-least-32-characters-long"
                        .getBytes(StandardCharsets.UTF_8)))
            .compact();

    assertThat(jwtUtil.validateToken(token, user)).isFalse();
  }

  @Test
  void weakJwtConfigurationFailsFast() {
    JwtUtil jwtUtil = jwtUtil(60_000L);
    ReflectionTestUtils.setField(jwtUtil, "secret", "too-short");

    assertThatThrownBy(jwtUtil::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("jwt.secret must contain at least 32 UTF-8 bytes");
  }

  @Test
  void filterIgnoresRequestsWithoutBearerTokens() throws Exception {
    JwtUtil jwtUtil = mock(JwtUtil.class);
    FilterChain chain = mock(FilterChain.class);
    JwtRequestFilter filter = new JwtRequestFilter(jwtUtil);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    request.addHeader("Authorization", "Basic credentials");
    filter.doFilter(request, response, chain);

    verify(chain, times(2)).doFilter(request, response);
  }

  @Test
  void filterClearsContextForMalformedBearerTokens() throws Exception {
    JwtUtil jwtUtil = mock(JwtUtil.class);
    FilterChain chain = mock(FilterChain.class);
    when(jwtUtil.validateAndExtractUsername("bad")).thenReturn(null);
    JwtRequestFilter filter = new JwtRequestFilter(jwtUtil);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer bad");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  void filterAuthenticatesValidBearerTokens() throws Exception {
    JwtUtil jwtUtil = mock(JwtUtil.class);
    FilterChain chain = mock(FilterChain.class);
    when(jwtUtil.validateAndExtractUsername("good")).thenReturn("alice");
    JwtRequestFilter filter = new JwtRequestFilter(jwtUtil);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer good");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
    verify(chain).doFilter(request, response);
  }

  @Test
  void filterLeavesRequestUnauthenticatedWhenTokenIsInvalidOrAlreadyAuthenticated()
      throws Exception {
    JwtUtil jwtUtil = mock(JwtUtil.class);
    FilterChain chain = mock(FilterChain.class);
    when(jwtUtil.validateAndExtractUsername("invalid")).thenReturn(null);
    JwtRequestFilter filter = new JwtRequestFilter(jwtUtil);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    filter.doFilter(request, response, chain);
    verify(chain, times(2)).doFilter(request, response);
  }

  private JwtUtil jwtUtil(long expiration) {
    JwtUtil jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(
        jwtUtil, "secret", "test-only-secret-that-is-at-least-32-characters-long");
    ReflectionTestUtils.setField(jwtUtil, "expiration", expiration);
    return jwtUtil;
  }
}
