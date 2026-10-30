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
import org.springframework.security.crypto.password.PasswordEncoder;
import xyz.subho.clone.twitter.entity.Follow;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.exception.ResourceNotFoundException;
import xyz.subho.clone.twitter.model.UserModel;
import xyz.subho.clone.twitter.model.UserUpdateRequest;
import xyz.subho.clone.twitter.repository.FollowRepository;
import xyz.subho.clone.twitter.repository.UsersRepository;
import xyz.subho.clone.twitter.service.impl.UserServiceImpl;
import xyz.subho.clone.twitter.utility.UserMapper;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UsersRepository usersRepository;
  @Mock private FollowRepository followRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserMapper userMapper;
  @Mock private RefreshTokenService refreshTokenService;

  private UserServiceImpl service;
  private final UUID userId = UUID.randomUUID();
  private final UUID followerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new UserServiceImpl(
            usersRepository, followRepository, passwordEncoder, userMapper, refreshTokenService);
  }

  @Test
  void addUserHashesPasswordBeforeSaving() {
    UserModel request = userModel("plain");
    Users entity = new Users();
    when(userMapper.toEntity(request)).thenReturn(entity);
    when(passwordEncoder.encode("plain")).thenReturn("hashed");
    when(usersRepository.save(entity)).thenReturn(entity);
    when(userMapper.toModel(entity)).thenReturn(request);

    assertThat(service.addUser(request)).isEqualTo(request);
    assertThat(entity.getPassword()).isEqualTo("hashed");
  }

  @Test
  void editUserChangesOnlyOwnProfileFieldsAndHashesOptionalPassword() {
    Users entity = user("alice");
    entity.setId(userId);
    UserUpdateRequest request =
        new UserUpdateRequest("Alice Updated", "alice@example.com", "avatar", "bio", "newpass");
    UserModel response = userModel(null);
    when(usersRepository.findByUsernameForUpdate("alice")).thenReturn(entity);
    when(passwordEncoder.encode("newpass")).thenReturn("new-hash");
    when(usersRepository.save(entity)).thenReturn(entity);
    when(userMapper.toModel(entity)).thenReturn(response);

    assertThat(service.editUser("alice", request)).isEqualTo(response);
    assertThat(entity.getUsername()).isEqualTo("alice");
    assertThat(entity.getPassword()).isEqualTo("new-hash");
    verify(refreshTokenService).deleteByUserId(entity.getId());
    assertThat(entity.getFollowerCount()).isZero();
    assertThat(entity.getVerified()).isFalse();
  }

  @Test
  void editUserKeepsPasswordWhenItIsOmitted() {
    Users entity = user("alice");
    entity.setPassword("old-hash");
    UserUpdateRequest request =
        new UserUpdateRequest("Alice", "alice@example.com", null, null, null);
    when(usersRepository.findByUsernameForUpdate("alice")).thenReturn(entity);
    when(usersRepository.save(entity)).thenReturn(entity);
    when(userMapper.toModel(entity)).thenReturn(userModel(null));

    service.editUser("alice", request);

    assertThat(entity.getPassword()).isEqualTo("old-hash");
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  void editUserKeepsPasswordWhenItIsBlank() {
    Users entity = user("alice");
    entity.setPassword("old-hash");
    when(usersRepository.findByUsernameForUpdate("alice")).thenReturn(entity);
    when(usersRepository.save(entity)).thenReturn(entity);
    when(userMapper.toModel(entity)).thenReturn(userModel(null));

    service.editUser("alice", new UserUpdateRequest("Alice", "a@example.com", null, null, " "));

    assertThat(entity.getPassword()).isEqualTo("old-hash");
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  void editUserRejectsUnknownUser() {
    when(usersRepository.findByUsernameForUpdate("missing")).thenReturn(null);
    UserUpdateRequest request = new UserUpdateRequest("Name", "a@example.com", null, null, null);

    assertThrows(ResourceNotFoundException.class, () -> service.editUser("missing", request));
  }

  @Test
  void getUserByIdReturnsNullForMissingUser() {
    when(usersRepository.findById(userId)).thenReturn(Optional.empty());

    assertThat(service.getUserByUserId(userId)).isNull();
    verify(userMapper, never()).toModel(any(Users.class));
  }

  @Test
  void getUserByNameMapsTheRepositoryResult() {
    UserModel response = userModel(null);
    Users entity = user("alice");
    when(usersRepository.findByUsername("alice")).thenReturn(entity);
    when(userMapper.toModel(entity)).thenReturn(response);

    assertThat(service.getUserByUserName("alice")).isEqualTo(response);
  }

  @Test
  void followIsIdempotentAndUpdatesBothCounters() {
    Users user = user("target");
    user.setId(userId);
    Users follower = user("follower");
    follower.setId(followerId);
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(usersRepository.findByIdForUpdate(followerId)).thenReturn(Optional.of(follower));
    when(followRepository.existsByFollowerAndFollowing(follower, user)).thenReturn(false);

    assertThat(service.addFollower(followerId, userId)).isTrue();
    assertThat(user.getFollowerCount()).isEqualTo(1);
    assertThat(follower.getFollowingCount()).isEqualTo(1);
    verify(followRepository).save(any(Follow.class));
  }

  @Test
  void duplicateFollowDoesNotChangeCounters() {
    Users user = user("target");
    Users follower = user("follower");
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(usersRepository.findByIdForUpdate(followerId)).thenReturn(Optional.of(follower));
    when(followRepository.existsByFollowerAndFollowing(follower, user)).thenReturn(true);

    assertThat(service.addFollower(followerId, userId)).isFalse();
    verify(followRepository, never()).save(any());
  }

  @Test
  void unfollowOnlyChangesCountersWhenAJoinRowWasDeleted() {
    Users user = user("target");
    user.setId(userId);
    user.setFollowerCount(2);
    Users follower = user("follower");
    follower.setId(followerId);
    follower.setFollowingCount(3L);
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(usersRepository.findByIdForUpdate(followerId)).thenReturn(Optional.of(follower));
    when(followRepository.deleteByFollowerAndFollowing(follower, user)).thenReturn(1);

    assertThat(service.removeFollower(followerId, userId)).isTrue();
    assertThat(user.getFollowerCount()).isEqualTo(1);
    assertThat(follower.getFollowingCount()).isEqualTo(2);

    when(followRepository.deleteByFollowerAndFollowing(follower, user)).thenReturn(0);
    assertThat(service.removeFollower(followerId, userId)).isFalse();
    assertThat(user.getFollowerCount()).isEqualTo(1);
  }

  @Test
  void selfFollowIsRejectedBeforeTakingLocks() {
    assertThrows(BadRequestException.class, () -> service.addFollower(userId, userId));
    assertThrows(BadRequestException.class, () -> service.removeFollower(userId, userId));
    verify(usersRepository, never()).findByIdForUpdate(any());
  }

  @Test
  void followLocksUsersInAscendingIdOrderAndReportsMissingUsers() {
    UUID lowerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID higherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    Users target = user("target");
    target.setId(lowerId);
    Users follower = user("follower");
    follower.setId(higherId);
    when(usersRepository.findByIdForUpdate(lowerId)).thenReturn(Optional.of(target));
    when(usersRepository.findByIdForUpdate(higherId)).thenReturn(Optional.of(follower));
    when(followRepository.existsByFollowerAndFollowing(follower, target)).thenReturn(false);

    assertThat(service.addFollower(higherId, lowerId)).isTrue();

    InOrder order = inOrder(usersRepository);
    order.verify(usersRepository).findByIdForUpdate(lowerId);
    order.verify(usersRepository).findByIdForUpdate(higherId);

    when(usersRepository.findByIdForUpdate(lowerId)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> service.addFollower(higherId, lowerId));
  }

  @Test
  void followAndUnfollowMapRolesWhenTheFollowerHasTheLowerId() {
    UUID lowerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID higherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    Users target = user("target");
    target.setId(higherId);
    Users follower = user("follower");
    follower.setId(lowerId);
    when(usersRepository.findByIdForUpdate(lowerId)).thenReturn(Optional.of(follower));
    when(usersRepository.findByIdForUpdate(higherId)).thenReturn(Optional.of(target));
    when(followRepository.existsByFollowerAndFollowing(follower, target)).thenReturn(false);
    when(followRepository.deleteByFollowerAndFollowing(follower, target)).thenReturn(1);

    assertThat(service.addFollower(lowerId, higherId)).isTrue();
    assertThat(service.removeFollower(lowerId, higherId)).isTrue();
  }

  @Test
  void unfollowNeverDecrementsCountersBelowZero() {
    Users target = user("target");
    target.setId(userId);
    Users follower = user("follower");
    follower.setId(followerId);
    when(usersRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(target));
    when(usersRepository.findByIdForUpdate(followerId)).thenReturn(Optional.of(follower));
    when(followRepository.deleteByFollowerAndFollowing(follower, target)).thenReturn(1);

    assertThat(service.removeFollower(followerId, userId)).isTrue();

    assertThat(target.getFollowerCount()).isZero();
    assertThat(follower.getFollowingCount()).isZero();
  }

  @Test
  void relationshipPagesMapTheCorrectSide() {
    Users user = user("target");
    user.setId(userId);
    Users follower = user("follower");
    follower.setId(followerId);
    Follow follow = new Follow(follower, user);
    UserModel followerModel = userModel(null);
    UserModel followingModel = userModel(null);
    when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
    when(followRepository.findByFollowing(any(), any()))
        .thenReturn(new PageImpl<>(List.of(follow)));
    when(followRepository.findByFollower(any(), any())).thenReturn(new PageImpl<>(List.of(follow)));
    when(userMapper.toModel(follower)).thenReturn(followerModel);
    when(userMapper.toModel(user)).thenReturn(followingModel);

    assertThat(service.getFollowers(userId, PageRequest.of(0, 10)).getContent())
        .containsExactly(followerModel);
    assertThat(service.getFollowings(userId, PageRequest.of(0, 10)).getContent())
        .containsExactly(followingModel);
  }

  @Test
  void relationshipPagesRejectUnknownUsers() {
    when(usersRepository.findById(userId)).thenReturn(Optional.empty());
    var pageable = PageRequest.of(0, 10);

    assertThrows(ResourceNotFoundException.class, () -> service.getFollowers(userId, pageable));
    assertThrows(ResourceNotFoundException.class, () -> service.getFollowings(userId, pageable));
  }

  private Users user(String username) {
    Users user = new Users();
    user.setUsername(username);
    user.setName(username);
    user.setEmail(username + "@example.com");
    user.setPassword("hash");
    return user;
  }

  private UserModel userModel(String password) {
    return new UserModel(
        userId, "alice", "Alice", password, "alice@example.com", null, null, 0L, 0L, false);
  }
}
