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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import xyz.subho.clone.twitter.entity.HashtagPosts;
import xyz.subho.clone.twitter.entity.Hashtags;
import xyz.subho.clone.twitter.entity.Likes;
import xyz.subho.clone.twitter.entity.Posts;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.exception.ResourceNotFoundException;
import xyz.subho.clone.twitter.model.PostModel;
import xyz.subho.clone.twitter.repository.HashtagPostsRepository;
import xyz.subho.clone.twitter.repository.HashtagsRepository;
import xyz.subho.clone.twitter.repository.LikesRepository;
import xyz.subho.clone.twitter.repository.PostsRepository;
import xyz.subho.clone.twitter.repository.UsersRepository;
import xyz.subho.clone.twitter.service.impl.PostServiceImpl;
import xyz.subho.clone.twitter.utility.PostMapper;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

  @Mock private PostsRepository postsRepository;
  @Mock private HashtagPostsRepository hashtagPostsRepository;
  @Mock private HashtagsRepository hashtagsRepository;
  @Mock private HashtagService hashtagService;
  @Mock private LikesRepository likesRepository;
  @Mock private UsersRepository usersRepository;
  @Mock private PostMapper postMapper;

  private PostServiceImpl service;
  private final UUID postId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new PostServiceImpl(
            postsRepository,
            hashtagPostsRepository,
            hashtagsRepository,
            hashtagService,
            likesRepository,
            usersRepository,
            postMapper);
  }

  @Test
  void addPostUsesServerOwnedIdentityCountersAndCreatesHashtagJoins() {
    PostModel request = postModel("client-id", 99L, 42L);
    Posts post = new Posts();
    post.setId(UUID.randomUUID());
    post.setLikeCount(0L);
    post.setRepostCount(0L);
    Users owner = user(userId);
    Hashtags hashtag = new Hashtags();
    hashtag.setTag("java");
    PostModel response = postModel(null, 0L, 0L);
    when(postMapper.toEntity(request)).thenReturn(post);
    when(usersRepository.getReferenceById(userId)).thenReturn(owner);
    when(postsRepository.save(post)).thenReturn(post);
    when(hashtagService.getHashtagsByTags(List.of("Java"))).thenReturn(List.of(hashtag));
    when(postMapper.toModel(post)).thenReturn(response);

    assertThat(service.addPost(request)).isEqualTo(response);
    assertThat(post.getUsers()).isEqualTo(owner);
    verify(hashtagPostsRepository).saveAll(any());
    HashtagPosts savedJoin = post.getPostHashtags().get(0);
    assertThat(savedJoin.getHashtags()).isEqualTo(hashtag);
    assertThat(savedJoin.getPosts()).isEqualTo(post);
  }

  @Test
  void deletePostRequiresOwnership() {
    Posts post = post(postId, userId);
    when(postsRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

    assertThrows(AccessDeniedException.class, () -> service.deletePost(postId, otherUserId));
    verify(postsRepository, never()).delete(post);

    assertThat(service.deletePost(postId, userId)).isTrue();
    verify(hashtagsRepository).decrementRecentPostCountForPost(postId);
    verify(hashtagPostsRepository).deleteByPosts(post);
    verify(likesRepository).deleteByPosts(post);
    verify(postsRepository).clearReferences(postId);
    verify(postsRepository).delete(post);
  }

  @Test
  void addLikeIsIdempotent() {
    Posts post = post(postId, userId);
    Users user = user(userId);
    when(postsRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
    when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
    when(likesRepository.existsByPostsAndUsers(post, user)).thenReturn(false, true);

    assertThat(service.addLike(postId, userId)).isEqualTo(1);
    assertThat(service.addLike(postId, userId)).isEqualTo(1);
    verify(likesRepository).save(any(Likes.class));
  }

  @Test
  void removeLikeOnlyDecrementsWhenAJoinRowWasDeleted() {
    Posts post = post(postId, userId);
    post.setLikeCount(2L);
    Users user = user(userId);
    when(postsRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
    when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
    when(likesRepository.deleteByPostsAndUsers(post, user)).thenReturn(1, 0);

    assertThat(service.removeLike(postId, userId)).isEqualTo(1);
    assertThat(service.removeLike(postId, userId)).isEqualTo(1);
  }

  @Test
  void missingPostAndUserAreRejected() {
    when(postsRepository.findById(postId)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> service.getPost(postId));
    when(postsRepository.findByIdForUpdate(postId)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> service.addLike(postId, userId));

    Posts post = post(postId, userId);
    when(postsRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
    when(usersRepository.findById(userId)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> service.removeLike(postId, userId));

    when(usersRepository.findById(userId)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> service.addLike(postId, userId));
  }

  @Test
  void missingReplyOrOriginalPostIsRejected() {
    PostModel request =
        new PostModel(
            null, "post", userId, List.of(), null, null, postId, null, null, List.of(), List.of());
    when(postsRepository.findByIdForUpdate(postId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.addPost(request));
    verify(postMapper, never()).toEntity(request);
  }

  @Test
  void referencedPostsAreLockedInUuidOrder() {
    UUID lowerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID higherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    PostModel request =
        new PostModel(
            null, "post", userId, List.of(), null, null, higherId, lowerId, null, List.of(),
            List.of());
    when(postsRepository.findByIdForUpdate(lowerId)).thenReturn(Optional.of(new Posts()));
    when(postsRepository.findByIdForUpdate(higherId)).thenReturn(Optional.of(new Posts()));
    Posts post = new Posts();
    when(postMapper.toEntity(request)).thenReturn(post);
    when(usersRepository.getReferenceById(userId)).thenReturn(user(userId));
    when(postsRepository.save(post)).thenReturn(post);
    when(hashtagService.getHashtagsByTags(List.of())).thenReturn(List.of());

    service.addPost(request);

    InOrder order = inOrder(postsRepository);
    order.verify(postsRepository).findByIdForUpdate(lowerId);
    order.verify(postsRepository).findByIdForUpdate(higherId);
  }

  @Test
  void getPostMapsAnExistingPost() {
    Posts post = post(postId, userId);
    PostModel model = postModel(null, 0L, 0L);
    when(postsRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postMapper.toModel(post)).thenReturn(model);

    assertThat(service.getPost(postId)).isEqualTo(model);
  }

  @Test
  void pagesAreMappedAndRepliesUseTheRepositoryFilter() {
    Posts post = post(postId, userId);
    PostModel model = postModel(null, 0L, 0L);
    when(postsRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(post)));
    when(postsRepository.existsById(postId)).thenReturn(true);
    when(postsRepository.findByReplyToId(postId, PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(post)));
    when(postMapper.toModel(post)).thenReturn(model);

    assertThat(service.getAllPosts(PageRequest.of(0, 10)).getContent()).containsExactly(model);
    assertThat(service.getReplies(postId, PageRequest.of(0, 10)).getContent())
        .containsExactly(model);
  }

  @Test
  void repliesRejectAnUnknownParentPost() {
    when(postsRepository.existsById(postId)).thenReturn(false);
    var pageable = PageRequest.of(0, 10);

    assertThrows(ResourceNotFoundException.class, () -> service.getReplies(postId, pageable));
    verify(postsRepository, never()).findByReplyToId(any(), any());
  }

  private Posts post(UUID id, UUID ownerId) {
    Posts post = new Posts();
    post.setId(id);
    post.setUsers(user(ownerId));
    post.setLikeCount(0L);
    post.setRepostCount(0L);
    return post;
  }

  private Users user(UUID id) {
    Users user = new Users();
    user.setId(id);
    user.setUsername("user-" + id);
    return user;
  }

  private PostModel postModel(String id, Long likes, Long reposts) {
    return new PostModel(
        id == null ? null : UUID.fromString("00000000-0000-0000-0000-000000000001"),
        "post",
        userId,
        List.of(),
        likes,
        reposts,
        null,
        null,
        null,
        id == null ? List.of() : List.of("Java"),
        List.of());
  }
}
