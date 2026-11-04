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

package xyz.subho.clone.twitter.service.impl;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.model.PostModel;
import xyz.subho.clone.twitter.model.SearchResultModel;
import xyz.subho.clone.twitter.model.UserSearchModel;
import xyz.subho.clone.twitter.repository.PostsRepository;
import xyz.subho.clone.twitter.repository.UsersRepository;
import xyz.subho.clone.twitter.service.SearchService;
import xyz.subho.clone.twitter.utility.PostMapper;
import xyz.subho.clone.twitter.utility.UserMapper;

@Service
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

  private static final int MAX_QUERY_LENGTH = 100;

  private final PostsRepository postsRepository;
  private final UsersRepository usersRepository;
  private final PostMapper postMapper;
  private final UserMapper userMapper;

  public SearchServiceImpl(
      PostsRepository postsRepository,
      UsersRepository usersRepository,
      PostMapper postMapper,
      UserMapper userMapper) {
    this.postsRepository = postsRepository;
    this.usersRepository = usersRepository;
    this.postMapper = postMapper;
    this.userMapper = userMapper;
  }

  @Override
  public @NonNull Page<PostModel> searchPosts(@NonNull String query, @NonNull Pageable pageable) {
    return executeSearchPosts(query, pageable);
  }

  @Override
  public @NonNull Page<UserSearchModel> searchUsers(
      @NonNull String query, @NonNull Pageable pageable) {
    return executeSearchUsers(query, pageable);
  }

  @Override
  public @NonNull SearchResultModel searchAll(@NonNull String query, @NonNull Pageable pageable) {
    Page<UserSearchModel> users = executeSearchUsers(query, pageable);
    Page<PostModel> posts = executeSearchPosts(query, pageable);
    return new SearchResultModel(users, posts);
  }

  private Page<PostModel> executeSearchPosts(String query, Pageable pageable) {
    String sanitizedQuery = validateAndSanitizeQuery(query);
    var postsPage = postsRepository.searchPosts(sanitizedQuery, pageable);
    return postsPage.map(postMapper::toModel);
  }

  private Page<UserSearchModel> executeSearchUsers(String query, Pageable pageable) {
    String sanitizedQuery = validateAndSanitizeQuery(query);
    if (sanitizedQuery.startsWith("@") && sanitizedQuery.length() > 1) {
      sanitizedQuery = sanitizedQuery.substring(1);
    }
    var usersPage = usersRepository.searchUsers(sanitizedQuery, pageable);
    return usersPage.map(userMapper::toSearchModel);
  }

  private String validateAndSanitizeQuery(String query) {
    if (query == null) {
      throw new BadRequestException("Search query must not be blank");
    }
    String trimmed = query.trim();
    if (trimmed.isEmpty()) {
      throw new BadRequestException("Search query must not be blank");
    }
    if (trimmed.length() > MAX_QUERY_LENGTH) {
      throw new BadRequestException(
          "Search query must not exceed " + MAX_QUERY_LENGTH + " characters");
    }
    return escapeLikeWildcards(trimmed);
  }

  private String escapeLikeWildcards(String input) {
    return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
