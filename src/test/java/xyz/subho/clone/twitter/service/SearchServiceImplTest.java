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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import xyz.subho.clone.twitter.entity.Posts;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.model.PostModel;
import xyz.subho.clone.twitter.model.SearchResultModel;
import xyz.subho.clone.twitter.model.UserSearchModel;
import xyz.subho.clone.twitter.repository.PostsRepository;
import xyz.subho.clone.twitter.repository.UsersRepository;
import xyz.subho.clone.twitter.service.impl.SearchServiceImpl;
import xyz.subho.clone.twitter.utility.PostMapper;
import xyz.subho.clone.twitter.utility.UserMapper;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

  @Mock private PostsRepository postsRepository;
  @Mock private UsersRepository usersRepository;
  @Mock private PostMapper postMapper;
  @Mock private UserMapper userMapper;

  private SearchServiceImpl searchService;
  private final Pageable pageable = PageRequest.of(0, 10);

  @BeforeEach
  void setUp() {
    searchService = new SearchServiceImpl(postsRepository, usersRepository, postMapper, userMapper);
  }

  @Test
  void blankOrNullQueriesThrowBadRequestException() {
    assertThatThrownBy(() -> searchService.searchPosts(null, pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Search query must not be blank");

    assertThatThrownBy(() -> searchService.searchPosts("", pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Search query must not be blank");

    assertThatThrownBy(() -> searchService.searchPosts("   ", pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Search query must not be blank");

    assertThatThrownBy(() -> searchService.searchUsers(null, pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Search query must not be blank");

    assertThatThrownBy(() -> searchService.searchUsers("   ", pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Search query must not be blank");

    verify(postsRepository, never()).searchPosts(any(), any());
    verify(usersRepository, never()).searchUsers(any(), any());
  }

  @Test
  void overlyLongQueriesThrowBadRequestException() {
    String longQuery = "a".repeat(101);

    assertThatThrownBy(() -> searchService.searchPosts(longQuery, pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Search query must not exceed 100 characters");

    assertThatThrownBy(() -> searchService.searchUsers(longQuery, pageable))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Search query must not exceed 100 characters");
  }

  @Test
  void searchPostsTrimsWhitespaceAndEscapesWildcards() {
    Posts postEntity = new Posts();
    PostModel postModel =
        new PostModel(
            UUID.randomUUID(),
            "100% win",
            UUID.randomUUID(),
            List.of(),
            0L,
            0L,
            null,
            null,
            null,
            List.of(),
            List.of());

    when(postsRepository.searchPosts(eq("100\\%\\_win\\\\"), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(postEntity)));
    when(postMapper.toModel(postEntity)).thenReturn(postModel);

    Page<PostModel> result = searchService.searchPosts("  100%_win\\  ", pageable);

    assertThat(result.getContent()).containsExactly(postModel);
    verify(postsRepository).searchPosts("100\\%\\_win\\\\", pageable);
  }

  @Test
  void searchUsersStripsLeadingAtSymbolAndEscapesWildcards() {
    Users userEntity = new Users();
    UserSearchModel userModel =
        new UserSearchModel(UUID.randomUUID(), "alice", "Alice Smith", null, null, 10L, 5L, false);

    when(usersRepository.searchUsers(eq("alice\\_smith"), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(userEntity)));
    when(userMapper.toSearchModel(userEntity)).thenReturn(userModel);

    Page<UserSearchModel> result = searchService.searchUsers(" @alice_smith ", pageable);

    assertThat(result.getContent()).containsExactly(userModel);
    verify(usersRepository).searchUsers("alice\\_smith", pageable);
  }

  @Test
  void searchUsersWithoutAtSymbolSearchesDirectly() {
    Users userEntity = new Users();
    UserSearchModel userModel =
        new UserSearchModel(UUID.randomUUID(), "bob", "Bob Jones", null, null, 2L, 1L, false);

    when(usersRepository.searchUsers(eq("bob"), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(userEntity)));
    when(userMapper.toSearchModel(userEntity)).thenReturn(userModel);

    Page<UserSearchModel> result = searchService.searchUsers("bob", pageable);

    assertThat(result.getContent()).containsExactly(userModel);
    verify(usersRepository).searchUsers("bob", pageable);
  }

  @Test
  void searchAllReturnsBothUsersAndPosts() {
    Users userEntity = new Users();
    UserSearchModel userModel =
        new UserSearchModel(UUID.randomUUID(), "coder", "Coder Guy", null, null, 100L, 50L, true);

    Posts postEntity = new Posts();
    PostModel postModel =
        new PostModel(
            UUID.randomUUID(),
            "Learning Java",
            UUID.randomUUID(),
            List.of(),
            5L,
            2L,
            null,
            null,
            null,
            List.of(),
            List.of());

    when(usersRepository.searchUsers(eq("java"), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(userEntity)));
    when(userMapper.toSearchModel(userEntity)).thenReturn(userModel);

    when(postsRepository.searchPosts(eq("java"), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(postEntity)));
    when(postMapper.toModel(postEntity)).thenReturn(postModel);

    SearchResultModel result = searchService.searchAll("java", pageable);

    assertThat(result.users().getContent()).containsExactly(userModel);
    assertThat(result.posts().getContent()).containsExactly(postModel);
  }

  @Test
  void emptyResultsReturnEmptyPages() {
    when(postsRepository.searchPosts(eq("unknown"), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));
    when(usersRepository.searchUsers(eq("unknown"), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    Page<PostModel> posts = searchService.searchPosts("unknown", pageable);
    Page<UserSearchModel> users = searchService.searchUsers("unknown", pageable);

    assertThat(posts).isEmpty();
    assertThat(users).isEmpty();
  }
}
