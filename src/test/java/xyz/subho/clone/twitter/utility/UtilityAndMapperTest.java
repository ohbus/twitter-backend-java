/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright © 2021-2026 Subhrodip Mohanta (hello@subho.xyz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package xyz.subho.clone.twitter.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import xyz.subho.clone.twitter.entity.Posts;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.model.PostModel;
import xyz.subho.clone.twitter.model.UserModel;

class UtilityAndMapperTest {

  @Test
  void utilityAcceptsOnlyUuidStrings() {
    Utility utility = new Utility();
    UUID id = UUID.randomUUID();

    assertThat(utility.isUUID(id.toString())).isTrue();
    assertThat(utility.isUUID("bad")).isFalse();
    assertThat(utility.converStringToUUID(id.toString())).isEqualTo(id);
    assertThat(utility.converStringToUUID("bad")).isNull();
  }

  @Test
  void postMapperHandlesNullAndNonNullCollections() {
    PostMapper mapper = Mappers.getMapper(PostMapper.class);
    assertThat(mapper.mapToKeysList(null)).isEmpty();
    assertThat(mapper.mapToKeysList(Map.of("java", Instant.now()))).containsExactly("java");
    assertThat(mapper.listToInstantMap(null)).isEmpty();
    assertThat(mapper.listToInstantMap(List.of("java"))).containsKey("java");
  }

  @Test
  void generatedPostMappingDoesNotTrustClientIdentityOrCounters() {
    PostMapper mapper = Mappers.getMapper(PostMapper.class);
    Posts post =
        mapper.toEntity(
            new PostModel(
                UUID.randomUUID(),
                "text",
                UUID.randomUUID(),
                List.of(),
                99L,
                88L,
                null,
                null,
                null,
                null,
                null));

    assertThat(post.getId()).isNull();
    assertThat(post.getLikeCount()).isZero();
    assertThat(post.getRepostCount()).isZero();
  }

  @Test
  void generatedUserMappingDoesNotTrustClientIdentityOrCounters() {
    UserMapper mapper = Mappers.getMapper(UserMapper.class);
    Users user =
        mapper.toEntity(
            new UserModel(
                UUID.randomUUID(),
                "alice",
                "Alice",
                "password",
                "alice@example.com",
                null,
                null,
                99L,
                88L,
                true));

    assertThat(user.getId()).isNull();
    assertThat(user.getFollowerCount()).isZero();
    assertThat(user.getFollowingCount()).isZero();
    assertThat(user.getVerified()).isFalse();
  }

  @Test
  void userEmailsAreBoundedToTheDatabaseColumnLength() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    var user =
        new UserModel(
            null,
            "alice",
            "Alice",
            "password",
            "a".repeat(245) + "@example.com",
            null,
            null,
            null,
            null,
            null);

    assertThat(validator.validate(user))
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("email"));
  }

  @Test
  void deepPageOffsetsAreRejectedBeforeQueryExecution() {
    var pageable = PageRequest.of(101, 100);
    assertThatThrownBy(() -> Utility.validatePageable(pageable, "createdAt", "id"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Page is too deep; use a smaller page number");
  }

  @Test
  void unsupportedSortPropertiesAreRejectedBeforeQueryExecution() {
    var pageable = PageRequest.of(0, 20, Sort.by("password"));
    assertThatThrownBy(() -> Utility.validatePageable(pageable, "createdAt", "id"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Unsupported sort property: password");
  }
}
