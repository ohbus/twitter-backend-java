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

package xyz.subho.clone.twitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void exportsAnAccurateOpenApiDocument() throws Exception {
    String document =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode openApi = objectMapper.readTree(document);
    var operations = openApi.at("/paths").valueStream().flatMap(JsonNode::valueStream).toList();
    Path output = Path.of("target", "generated-docs", "openapi.json");
    Files.createDirectories(output.getParent());
    Files.writeString(output, document);

    assertThat(openApi.at("/info/title").asString()).isEqualTo("Moo Twitter Backend API");
    assertThat(openApi.at("/info/version").asString()).isEqualTo("v1");
    assertThat(operations)
        .hasSize(23)
        .allMatch(operation -> !operation.path("summary").asString().isBlank());
    assertThat(openApi.at("/components/securitySchemes/bearerAuth/type").asString())
        .isEqualTo("http");
    assertThat(
            openApi.at("/components/schemas/UserModel/properties/password/writeOnly").asBoolean())
        .isTrue();
    assertThat(openApi.at("/paths/~1v1~1csrf/get/security").isEmpty()).isTrue();
    assertThat(openApi.at("/paths/~1v1~1authenticate/post/security/0/bearerAuth").isMissingNode())
        .isTrue();
    assertThat(openApi.at("/paths/~1v1~1authenticate/post/security/0/csrfCookie").isArray())
        .isTrue();
    assertThat(openApi.at("/paths/~1v1~1posts/get/security/0/bearerAuth").isArray()).isTrue();
    assertThat(openApi.at("/paths/~1v1~1posts/post/security/0/csrfHeader").isArray()).isTrue();
    assertThat(openApi.at("/paths/~1v1~1users/patch/security/0/bearerAuth").isArray()).isTrue();
    assertThat(openApi.at("/paths/~1v1~1users~1{userId}~1follow/put/responses/204").isMissingNode())
        .isFalse();
    assertThat(
            openApi
                .at("/paths/~1v1~1users~1{userId}~1follow/put/responses/204/content")
                .isMissingNode())
        .isTrue();
    assertThat(
            openApi.at("/paths/~1v1~1users~1{userId}~1follow/delete/responses/204").isMissingNode())
        .isFalse();
    assertThat(
            openApi
                .at("/paths/~1v1~1users~1{userId}~1follow/delete/responses/204/content")
                .isMissingNode())
        .isTrue();
    assertThat(openApi.at("/paths/~1v1~1posts~1{postId}/delete/responses/200").isMissingNode())
        .isFalse();
    assertThat(
            openApi.at("/paths/~1v1~1posts~1{postId}/delete/responses/200/content").isMissingNode())
        .isTrue();
  }
}
