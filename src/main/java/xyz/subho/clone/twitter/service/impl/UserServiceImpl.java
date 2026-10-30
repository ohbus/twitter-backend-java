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

package xyz.subho.clone.twitter.service.impl;

import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.subho.clone.twitter.entity.Follow;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.exception.ResourceNotFoundException;
import xyz.subho.clone.twitter.model.UserModel;
import xyz.subho.clone.twitter.model.UserUpdateRequest;
import xyz.subho.clone.twitter.repository.FollowRepository;
import xyz.subho.clone.twitter.repository.UsersRepository;
import xyz.subho.clone.twitter.service.RefreshTokenService;
import xyz.subho.clone.twitter.service.UserService;
import xyz.subho.clone.twitter.utility.UserMapper;

@Service
public class UserServiceImpl implements UserService {

  private static final String INVALID_USER_MESSAGE = "User is Invalid";

  private final UsersRepository usersRepository;
  private final FollowRepository followRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final RefreshTokenService refreshTokenService;

  public UserServiceImpl(
      UsersRepository usersRepository,
      FollowRepository followRepository,
      PasswordEncoder passwordEncoder,
      UserMapper userMapper,
      RefreshTokenService refreshTokenService) {
    this.usersRepository = usersRepository;
    this.followRepository = followRepository;
    this.passwordEncoder = passwordEncoder;
    this.userMapper = userMapper;
    this.refreshTokenService = refreshTokenService;
  }

  @Override
  public @Nullable UserModel getUserByUserName(@NonNull String username) {
    return userMapper.toModel(usersRepository.findByUsername(username));
  }

  @Override
  public @Nullable UserModel getUserByUserId(@NonNull UUID userId) {
    return usersRepository.findById(userId).map(userMapper::toModel).orElse(null);
  }

  @Override
  @Transactional
  public @NonNull UserModel addUser(@NonNull UserModel userModel) {
    var user = userMapper.toEntity(userModel);
    user.setPassword(passwordEncoder.encode(userModel.password()));
    return userMapper.toModel(usersRepository.save(user));
  }

  @Override
  @Transactional
  public @NonNull UserModel editUser(
      @NonNull String username, @NonNull UserUpdateRequest userUpdateRequest) {
    Users users = usersRepository.findByUsernameForUpdate(username);
    if (users == null) {
      throw new ResourceNotFoundException(INVALID_USER_MESSAGE);
    }

    users.setName(userUpdateRequest.name());
    users.setEmail(userUpdateRequest.email());
    users.setAvatar(userUpdateRequest.avatar());
    users.setBio(userUpdateRequest.bio());
    if (userUpdateRequest.password() != null && !userUpdateRequest.password().isBlank()) {
      users.setPassword(passwordEncoder.encode(userUpdateRequest.password()));
      refreshTokenService.deleteByUserId(users.getId());
    }
    return userMapper.toModel(usersRepository.save(users));
  }

  @Override
  @Transactional
  public boolean addFollower(@NonNull UUID followerId, @NonNull UUID userId) {
    if (followerId.equals(userId)) {
      throw new BadRequestException("You cannot follow yourself");
    }
    Users first = lockUser(firstId(followerId, userId), INVALID_USER_MESSAGE);
    Users second = lockUser(secondId(followerId, userId), "Follower is Invalid");
    Users user = userId.equals(first.getId()) ? first : second;
    Users follower = followerId.equals(first.getId()) ? first : second;
    if (followRepository.existsByFollowerAndFollowing(follower, user)) {
      return false;
    }

    Follow follow = new Follow(follower, user);
    followRepository.save(follow);

    user.setFollowerCount(user.getFollowerCount() + 1);
    usersRepository.save(user);

    follower.setFollowingCount(follower.getFollowingCount() + 1);
    usersRepository.save(follower);
    return true;
  }

  @Override
  @Transactional
  public boolean removeFollower(@NonNull UUID followerId, @NonNull UUID userId) {
    if (followerId.equals(userId)) {
      throw new BadRequestException("You cannot unfollow yourself");
    }
    Users first = lockUser(firstId(followerId, userId), INVALID_USER_MESSAGE);
    Users second = lockUser(secondId(followerId, userId), "Follower is Invalid");
    Users user = userId.equals(first.getId()) ? first : second;
    Users follower = followerId.equals(first.getId()) ? first : second;

    int deleted = followRepository.deleteByFollowerAndFollowing(follower, user);
    if (deleted == 0) {
      return false;
    }

    user.setFollowerCount(Math.max(0, user.getFollowerCount() - 1));
    usersRepository.save(user);

    follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
    usersRepository.save(follower);
    return true;
  }

  private Users lockUser(UUID userId, String message) {
    return usersRepository
        .findByIdForUpdate(userId)
        .orElseThrow(() -> new ResourceNotFoundException(message));
  }

  private UUID firstId(UUID left, UUID right) {
    return left.compareTo(right) < 0 ? left : right;
  }

  private UUID secondId(UUID left, UUID right) {
    return left.compareTo(right) < 0 ? right : left;
  }

  @Override
  @Transactional(readOnly = true)
  public @NonNull Page<UserModel> getFollowers(@NonNull UUID userId, @NonNull Pageable pageable) {
    Users user =
        usersRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(INVALID_USER_MESSAGE));
    var followsPage = followRepository.findByFollowing(user, pageable);
    return followsPage.map(f -> userMapper.toModel(f.getFollower()));
  }

  @Override
  @Transactional(readOnly = true)
  public @NonNull Page<UserModel> getFollowings(@NonNull UUID userId, @NonNull Pageable pageable) {
    Users user =
        usersRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(INVALID_USER_MESSAGE));
    var followsPage = followRepository.findByFollower(user, pageable);
    return followsPage.map(f -> userMapper.toModel(f.getFollowing()));
  }
}
