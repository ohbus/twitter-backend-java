/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright © 2021-2026 Subhrodip Mohanta (hello@subho.xyz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package xyz.subho.clone.twitter.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.ServletWebRequest;

class ApplicationErrorTest {

  @Test
  void errorAttributesRemoveSensitiveFrameworkDetails() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
    request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/broken");

    var attributes =
        new ApplicationErrorAttributes()
            .getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.defaults());

    assertThat(attributes)
        .containsEntry("locale", "en")
        .containsEntry("cause", "Request failed")
        .containsEntry("status", "500")
        .doesNotContainKeys("error", "message", "exception", "trace");
  }

  @Test
  void jsonErrorUsesTheRequestStatusAndReturnsTheErrorBody() {
    ErrorAttributes errorAttributes = mock(ErrorAttributes.class);
    when(errorAttributes.getErrorAttributes(any(), any()))
        .thenReturn(Map.of("status", "404", "cause", "Request failed"));
    ApplicationErrorController controller =
        new ApplicationErrorController(errorAttributes, new WebProperties());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);

    var response = controller.jsonError(request);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody()).containsEntry("cause", "Request failed");
  }

  @Test
  void jsonErrorIsSelectedThroughMvcForJsonRequests() throws Exception {
    ErrorAttributes errorAttributes = mock(ErrorAttributes.class);
    when(errorAttributes.getErrorAttributes(any(), any()))
        .thenReturn(Map.of("status", "404", "cause", "Request failed"));
    MockMvc mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ApplicationErrorController(errorAttributes, new WebProperties()))
            .build();

    mockMvc
        .perform(
            get("/error")
                .accept(MediaType.APPLICATION_JSON)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.cause").value("Request failed"));
  }
}
