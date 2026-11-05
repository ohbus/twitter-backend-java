/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright © 2021-2023 Subhrodip Mohanta (hello@subho.xyz)
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

package xyz.subho.clone.twitter.controller;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.subho.clone.twitter.constant.UserV1Constants;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.exception.ResourceNotFoundException;
import xyz.subho.clone.twitter.model.UserModel;
import xyz.subho.clone.twitter.model.UserUpdateRequest;
import xyz.subho.clone.twitter.service.UserService;
import xyz.subho.clone.twitter.utility.Utility;

@RestController
@RequestMapping(UserV1Constants.BASE_PATH)
@Timed(value = "moo.users.timer", description = "Time taken to process user requests")
@Tag(name = "Users", description = "Profiles and social relationships")
public class UserController {

  private final UserService userService;
  private final Utility utility;

  public UserController(UserService userService, Utility utility) {
    this.userService = userService;
    this.utility = utility;
  }

  @GetMapping(UserV1Constants.USER_ID_OR_NAME)
  @Operation(summary = "Get a user by UUID or @username")
  @ApiResponse(responseCode = "200", description = "User found")
  public ResponseEntity<UserModel> getUserByUserIdOrUserName(
      @PathVariable("userNameOrUserId") String userNameOrUserId) {

    UserModel userResponse;

    if (userNameOrUserId.startsWith("@")) {
      var username = userNameOrUserId.substring(1);
      userResponse = userService.getUserByUserName(username);
      if (userResponse == null) {
        throw new ResourceNotFoundException("User is Invalid");
      }
      return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    var userId = utility.converStringToUUID(userNameOrUserId);
    if (userId == null) {
      throw new BadRequestException("User ID is invalid");
    }
    userResponse = userService.getUserByUserId(userId);
    if (userResponse == null) {
      throw new ResourceNotFoundException("User is Invalid");
    }
    return new ResponseEntity<>(userResponse, HttpStatus.OK);
  }

  @PostMapping
  @Counted(value = "moo.users.signup", description = "Number of user signups")
  @Operation(summary = "Register a user")
  @ApiResponse(responseCode = "201", description = "User created")
  public ResponseEntity<UserModel> createUser(@Valid @RequestBody UserModel userResponse) {
    var user = userService.addUser(userResponse);
    return new ResponseEntity<>(user, HttpStatus.CREATED);
  }

  @PatchMapping
  @Timed(value = "moo.users.update", description = "Time taken to update user profile")
  @Operation(summary = "Update the authenticated user profile")
  @ApiResponse(responseCode = "200", description = "Profile updated")
  public ResponseEntity<UserModel> updateUser(
      @Valid @RequestBody UserUpdateRequest userUpdateRequest, Principal principal) {
    if (userService.getUserByUserName(principal.getName()) == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return ResponseEntity.ok(userService.editUser(principal.getName(), userUpdateRequest));
  }

  @PutMapping(UserV1Constants.FOLLOW)
  @Counted(value = "moo.users.follow", description = "Number of follow actions")
  @Operation(summary = "Follow a user")
  @ApiResponse(responseCode = "204", description = "User followed")
  public ResponseEntity<Void> addFollower(@PathVariable UUID userId, Principal principal) {
    var follower = userService.getUserByUserName(principal.getName());
    if (follower == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    userService.addFollower(Objects.requireNonNull(follower.id()), userId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @DeleteMapping(UserV1Constants.FOLLOW)
  @Operation(summary = "Unfollow a user")
  @ApiResponse(responseCode = "204", description = "User unfollowed")
  public ResponseEntity<Void> removeFollower(
      @PathVariable("userId") UUID userId, Principal principal) {
    var follower = userService.getUserByUserName(principal.getName());
    if (follower == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    userService.removeFollower(Objects.requireNonNull(follower.id()), userId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping(UserV1Constants.FOLLOWERS)
  @Operation(summary = "List a user's followers")
  @ApiResponse(responseCode = "200", description = "Page of followers")
  public Page<UserModel> getFollowers(
      @PathVariable("userId") UUID userId,
      @PageableDefault(
              size = 20,
              sort = {"createdAt", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    Utility.validatePageable(pageable, "createdAt", "id");
    return userService.getFollowers(userId, pageable);
  }

  @GetMapping(UserV1Constants.FOLLOWINGS)
  @Operation(summary = "List users followed by a user")
  @ApiResponse(responseCode = "200", description = "Page of followed users")
  public Page<UserModel> getFollowings(
      @PathVariable("userId") UUID userId,
      @PageableDefault(
              size = 20,
              sort = {"createdAt", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    Utility.validatePageable(pageable, "createdAt", "id");
    return userService.getFollowings(userId, pageable);
  }
}
