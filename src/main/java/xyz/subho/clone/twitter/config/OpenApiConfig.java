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

package xyz.subho.clone.twitter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  public static final String BEARER_AUTH = "bearerAuth";
  public static final String CSRF_COOKIE = "csrfCookie";
  public static final String CSRF_HEADER = "csrfHeader";

  private static final Set<String> PUBLIC_GET_PATHS =
      Set.of(
          "/v1/csrf",
          "/v1/users/{userNameOrUserId}",
          "/v1/users/{userId}/followers",
          "/v1/users/{userId}/followings");
  private static final Set<String> PUBLIC_WRITE_PATHS =
      Set.of("/v1/authenticate", "/v1/refresh", "/v1/users");

  @Bean
  public OpenAPI mooOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Moo Twitter Backend API")
                .version("v1")
                .description(
                    "Version 1 REST API for users, posts, social relationships, discovery, and authentication.")
                .contact(
                    new Contact()
                        .name("Moo maintainers")
                        .url("https://github.com/ohbus/twitter-backend-java"))
                .license(
                    new License()
                        .name("GNU AGPL-3.0")
                        .url("https://github.com/ohbus/twitter-backend-java/blob/master/LICENSE")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT returned by POST /v1/authenticate or POST /v1/refresh"))
                .addSecuritySchemes(
                    CSRF_COOKIE,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("XSRF-TOKEN")
                        .description("Cookie initialized by GET /v1/csrf"))
                .addSecuritySchemes(
                    CSRF_HEADER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-XSRF-TOKEN")
                        .description("Header containing the XSRF-TOKEN cookie value")));
  }

  @Bean
  public OpenApiCustomizer operationSecurityCustomizer() {
    return openApi ->
        openApi
            .getPaths()
            .forEach(
                (path, pathItem) ->
                    pathItem
                        .readOperationsMap()
                        .forEach(
                            (method, operation) ->
                                operation.setSecurity(operationSecurity(path, method))));
  }

  private List<SecurityRequirement> operationSecurity(String path, PathItem.HttpMethod method) {
    boolean write = method != PathItem.HttpMethod.GET;
    boolean publicOperation =
        write
            ? method == PathItem.HttpMethod.POST && PUBLIC_WRITE_PATHS.contains(path)
            : PUBLIC_GET_PATHS.contains(path);
    var security = new SecurityRequirement();
    if (!publicOperation) {
      security.addList(BEARER_AUTH);
    }
    if (write) {
      security.addList(CSRF_COOKIE);
      security.addList(CSRF_HEADER);
    }
    return security.isEmpty() ? List.of() : List.of(security);
  }
}
