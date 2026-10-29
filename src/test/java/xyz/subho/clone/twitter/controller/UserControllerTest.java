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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.exception.ResourceNotFoundException;
import xyz.subho.clone.twitter.model.UserModel;
import xyz.subho.clone.twitter.model.UserUpdateRequest;
import xyz.subho.clone.twitter.service.UserService;
import xyz.subho.clone.twitter.utility.Utility;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock private UserService userService;
  @Mock private Utility utility;

  private UserController controller;
  private final UUID userId = UUID.randomUUID();
  private final UUID otherId = UUID.randomUUID();
  private final Principal principal = () -> "alice";
  private final UserModel user =
      new UserModel(userId, "alice", "Alice", null, "a@e.test", null, null, 0L, 0L, false);

  @BeforeEach
  void setUp() {
    controller = new UserController(userService, utility);
  }

  @Test
  void lookupSupportsUsernameAndUuid() {
    when(userService.getUserByUserName("alice")).thenReturn(user);
    when(utility.converStringToUUID(otherId.toString())).thenReturn(otherId);
    when(userService.getUserByUserId(otherId)).thenReturn(user);

    assertThat(controller.getUserByUserIdOrUserName("@alice").getBody()).isEqualTo(user);
    assertThat(controller.getUserByUserIdOrUserName(otherId.toString()).getBody()).isEqualTo(user);
  }

  @Test
  void lookupRejectsMalformedAndUnknownIdentities() {
    when(utility.converStringToUUID("not-a-uuid")).thenReturn(null);
    assertThrows(
        BadRequestException.class, () -> controller.getUserByUserIdOrUserName("not-a-uuid"));

    when(userService.getUserByUserName("missing")).thenReturn(null);
    assertThrows(
        ResourceNotFoundException.class, () -> controller.getUserByUserIdOrUserName("@missing"));

    when(utility.converStringToUUID(otherId.toString())).thenReturn(otherId);
    when(userService.getUserByUserId(otherId)).thenReturn(null);
    assertThrows(
        ResourceNotFoundException.class,
        () -> controller.getUserByUserIdOrUserName(otherId.toString()));
  }

  @Test
  void createAndUpdateDelegateWithExpectedStatus() {
    when(userService.addUser(user)).thenReturn(user);
    when(userService.editUser(
            "alice", new UserUpdateRequest("Alice", "a@e.test", null, null, null)))
        .thenReturn(user);
    when(userService.getUserByUserName("alice")).thenReturn(user);

    assertThat(controller.createUser(user).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    var response =
        controller.updateUser(
            new UserUpdateRequest("Alice", "a@e.test", null, null, null), principal);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(user);
  }

  @Test
  void profileUpdateRejectsAStaleAuthenticatedPrincipal() {
    when(userService.getUserByUserName("alice")).thenReturn(null);

    var response =
        controller.updateUser(
            new UserUpdateRequest("Alice", "a@e.test", null, null, null), principal);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(userService, never())
        .editUser("alice", new UserUpdateRequest("Alice", "a@e.test", null, null, null));
  }

  @Test
  void followAndUnfollowRequireAProfileAndDelegateKnownProfiles() {
    when(userService.getUserByUserName("alice")).thenReturn(null);
    assertThat(controller.addFollower(otherId, principal).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(controller.removeFollower(otherId, principal).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(userService, never()).addFollower(any(), any());
    verify(userService, never()).removeFollower(any(), any());

    when(userService.getUserByUserName("alice")).thenReturn(user);
    controller.addFollower(otherId, principal);
    controller.removeFollower(otherId, principal);
    verify(userService).addFollower(userId, otherId);
    verify(userService).removeFollower(userId, otherId);
  }

  @Test
  void relationshipEndpointsDelegate() {
    when(userService.getFollowers(userId, PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(user)));
    when(userService.getFollowings(userId, PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(user)));

    assertThat(controller.getFollowers(userId, PageRequest.of(0, 10)).getContent())
        .containsExactly(user);
    assertThat(controller.getFollowings(userId, PageRequest.of(0, 10)).getContent())
        .containsExactly(user);
  }
}
