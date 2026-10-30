/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright © 2021-2026 Subhrodip Mohanta (hello@subho.xyz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package xyz.subho.clone.twitter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import xyz.subho.clone.twitter.constant.AuthV1Constants;
import xyz.subho.clone.twitter.constant.PostV1Constants;
import xyz.subho.clone.twitter.constant.UserV1Constants;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityMvcTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void profilePatchRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            patch(UserV1Constants.BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(csrfCookie())
                .header("X-XSRF-TOKEN", "test-token")
                .content(
                    "{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"password\":\"password\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshEndpointIsPublicButInvalidTokensAreRejectedByTheController() throws Exception {
    mockMvc
        .perform(
            post(AuthV1Constants.BASE_PATH + AuthV1Constants.REFRESH)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(csrfCookie())
                .header("X-XSRF-TOKEN", "test-token")
                .content("{\"refreshToken\":\"missing\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void csrfEndpointProvidesTokenAndCookieForStateChangingRequests() throws Exception {
    MvcResult csrfResult =
        mockMvc
            .perform(get(AuthV1Constants.BASE_PATH + AuthV1Constants.CSRF))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn();
    Cookie csrfCookie = Objects.requireNonNull(csrfResult.getResponse().getCookie("XSRF-TOKEN"));

    mockMvc
        .perform(
            post(AuthV1Constants.BASE_PATH + AuthV1Constants.REFRESH)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .content("{\"refreshToken\":\"missing\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void stateChangingRequestsRequireCsrfToken() throws Exception {
    mockMvc
        .perform(
            post(AuthV1Constants.BASE_PATH + AuthV1Constants.REFRESH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"missing\"}"))
        .andExpect(status().isForbidden());
  }

  private Cookie csrfCookie() {
    return new Cookie("XSRF-TOKEN", "test-token");
  }

  @Test
  void malformedBearerTokensDoNotBecomeServerErrors() throws Exception {
    mockMvc
        .perform(get(PostV1Constants.BASE_PATH).header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void openApiDocumentIsPublicAndAvailable() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").isString());
  }
}
