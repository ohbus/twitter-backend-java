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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.subho.clone.twitter.constant.SearchV1Constants;
import xyz.subho.clone.twitter.exception.ControllerExceptionHandler;
import xyz.subho.clone.twitter.model.PostModel;
import xyz.subho.clone.twitter.model.SearchResultModel;
import xyz.subho.clone.twitter.model.UserSearchModel;
import xyz.subho.clone.twitter.service.SearchService;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

  @Mock private SearchService searchService;

  private SearchController controller;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    controller = new SearchController(searchService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new ControllerExceptionHandler())
            .build();
  }

  @Test
  void searchPostsReturnsPaginatedResultsWithDefaultPagination() throws Exception {
    UUID postId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    PostModel post =
        new PostModel(
            postId,
            "Search test",
            userId,
            List.of(),
            0L,
            0L,
            null,
            null,
            null,
            List.of(),
            List.of());
    var defaultPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    when(searchService.searchPosts("test", defaultPageable))
        .thenReturn(new PageImpl<>(List.of(post), defaultPageable, 1));

    mockMvc
        .perform(get(SearchV1Constants.BASE_PATH + SearchV1Constants.POSTS).param("q", "test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(postId.toString()))
        .andExpect(jsonPath("$.content[0].text").value("Search test"))
        .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0));

    verify(searchService).searchPosts("test", defaultPageable);
  }

  @Test
  void searchPostsWithCustomPaginationAndSortingAppendsIdTieBreaker() throws Exception {
    var expectedPageable =
        PageRequest.of(1, 5, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.desc("id")));
    when(searchService.searchPosts(eq("test"), eq(expectedPageable)))
        .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

    mockMvc
        .perform(
            get(SearchV1Constants.BASE_PATH + SearchV1Constants.POSTS)
                .param("q", "test")
                .param("page", "1")
                .param("size", "5")
                .param("sort", "createdAt,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.number").value(1));

    verify(searchService).searchPosts("test", expectedPageable);
  }

  @Test
  void searchUsersReturnsPaginatedResultsWithDefaultPagination() throws Exception {
    UUID userId = UUID.randomUUID();
    UserSearchModel user =
        new UserSearchModel(userId, "alice", "Alice Smith", null, null, 1L, 1L, false);
    var defaultPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    when(searchService.searchUsers("alice", defaultPageable))
        .thenReturn(new PageImpl<>(List.of(user), defaultPageable, 1));

    mockMvc
        .perform(get(SearchV1Constants.BASE_PATH + SearchV1Constants.USERS).param("q", "alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(userId.toString()))
        .andExpect(jsonPath("$.content[0].username").value("alice"))
        .andExpect(jsonPath("$.content[0].name").value("Alice Smith"))
        .andExpect(jsonPath("$.content[0].email").doesNotExist())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0));

    verify(searchService).searchUsers("alice", defaultPageable);
  }

  @Test
  void searchUsersWithCustomSortWithoutIdAppendsIdTieBreaker() throws Exception {
    var expectedPageable =
        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name"), Sort.Order.desc("id")));
    when(searchService.searchUsers(eq("alice"), eq(expectedPageable)))
        .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

    mockMvc
        .perform(
            get(SearchV1Constants.BASE_PATH + SearchV1Constants.USERS)
                .param("q", "alice")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.number").value(0));

    verify(searchService).searchUsers("alice", expectedPageable);
  }

  @Test
  void searchUsersWithCustomSortContainingIdPreservesSortOrder() throws Exception {
    var expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("id")));
    when(searchService.searchUsers(eq("alice"), eq(expectedPageable)))
        .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

    mockMvc
        .perform(
            get(SearchV1Constants.BASE_PATH + SearchV1Constants.USERS)
                .param("q", "alice")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,asc"))
        .andExpect(status().isOk());

    verify(searchService).searchUsers("alice", expectedPageable);
  }

  @Test
  void searchAllReturnsCombinedUsersAndPosts() throws Exception {
    UUID userId = UUID.randomUUID();
    UserSearchModel user =
        new UserSearchModel(userId, "coder", "Coder Guy", null, null, 100L, 50L, true);

    UUID postId = UUID.randomUUID();
    PostModel post =
        new PostModel(
            postId,
            "Learning Java",
            userId,
            List.of(),
            5L,
            2L,
            null,
            null,
            null,
            List.of(),
            List.of());

    var defaultPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    SearchResultModel searchResult =
        new SearchResultModel(
            new PageImpl<>(List.of(user), defaultPageable, 1),
            new PageImpl<>(List.of(post), defaultPageable, 1));
    when(searchService.searchAll("tech", defaultPageable)).thenReturn(searchResult);

    mockMvc
        .perform(get(SearchV1Constants.BASE_PATH).param("q", "tech"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users.content[0].username").value("coder"))
        .andExpect(jsonPath("$.users.content[0].name").value("Coder Guy"))
        .andExpect(jsonPath("$.users.content[0].email").doesNotExist())
        .andExpect(jsonPath("$.posts.content[0].text").value("Learning Java"))
        .andExpect(jsonPath("$.users.totalElements").value(1))
        .andExpect(jsonPath("$.posts.totalElements").value(1));

    verify(searchService).searchAll("tech", defaultPageable);
  }

  @Test
  void missingQueryParameterReturnsBadRequest() throws Exception {
    mockMvc
        .perform(get(SearchV1Constants.BASE_PATH + SearchV1Constants.POSTS))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.statusCode").value(400))
        .andExpect(jsonPath("$.message").value("Malformed request"));
  }

  @Test
  void emptyResultsReturnsEmptyPageWithZeroTotalElements() throws Exception {
    var defaultPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    when(searchService.searchPosts("nonexistent", defaultPageable))
        .thenReturn(new PageImpl<>(List.of(), defaultPageable, 0));

    mockMvc
        .perform(
            get(SearchV1Constants.BASE_PATH + SearchV1Constants.POSTS).param("q", "nonexistent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void invalidSortFieldForPostsThrowsBadRequest() throws Exception {
    mockMvc
        .perform(
            get(SearchV1Constants.BASE_PATH + SearchV1Constants.POSTS)
                .param("q", "test")
                .param("sort", "password"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.statusCode").value(400))
        .andExpect(jsonPath("$.message").value("Unsupported sort property: password"));
  }

  @Test
  void invalidSortFieldForUsersThrowsBadRequest() throws Exception {
    mockMvc
        .perform(
            get(SearchV1Constants.BASE_PATH + SearchV1Constants.USERS)
                .param("q", "test")
                .param("sort", "email"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.statusCode").value(400))
        .andExpect(jsonPath("$.message").value("Unsupported sort property: email"));
  }

  @Test
  void pageOffsetTooDeepThrowsBadRequest() throws Exception {
    mockMvc
        .perform(
            get(SearchV1Constants.BASE_PATH + SearchV1Constants.POSTS)
                .param("q", "test")
                .param("page", "1001")
                .param("size", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.statusCode").value(400))
        .andExpect(jsonPath("$.message").value("Page is too deep; use a smaller page number"));
  }
}
