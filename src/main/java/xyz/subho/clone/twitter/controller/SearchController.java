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

package xyz.subho.clone.twitter.controller;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.subho.clone.twitter.constant.SearchV1Constants;
import xyz.subho.clone.twitter.model.PostModel;
import xyz.subho.clone.twitter.model.SearchResultModel;
import xyz.subho.clone.twitter.model.UserSearchModel;
import xyz.subho.clone.twitter.service.SearchService;
import xyz.subho.clone.twitter.utility.Utility;

@RestController
@RequestMapping(SearchV1Constants.BASE_PATH)
@Timed(value = "moo.search.timer", description = "Time taken to process search requests")
@Tag(name = "Search", description = "User and post search")
public class SearchController {

  private static final String CREATED_AT = "createdAt";
  private static final String ID = "id";

  private final SearchService searchService;

  public SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  @GetMapping
  @Counted(value = "moo.search.all", description = "Number of combined search requests")
  @Operation(summary = "Search users and posts")
  @ApiResponse(responseCode = "200", description = "Combined search results")
  public ResponseEntity<SearchResultModel> searchAll(
      @RequestParam("q") String query,
      @PageableDefault(
              size = 20,
              sort = {CREATED_AT, ID},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    Utility.validatePageable(pageable, CREATED_AT, ID);
    SearchResultModel result = searchService.searchAll(query, ensureIdSort(pageable));
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @GetMapping(SearchV1Constants.POSTS)
  @Counted(value = "moo.search.posts", description = "Number of post search requests")
  @Operation(summary = "Search posts")
  @ApiResponse(responseCode = "200", description = "Page of matching posts")
  public ResponseEntity<Page<PostModel>> searchPosts(
      @RequestParam("q") String query,
      @PageableDefault(
              size = 20,
              sort = {CREATED_AT, ID},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    Utility.validatePageable(pageable, CREATED_AT, ID);
    Page<PostModel> posts = searchService.searchPosts(query, ensureIdSort(pageable));
    return new ResponseEntity<>(posts, HttpStatus.OK);
  }

  @GetMapping(SearchV1Constants.USERS)
  @Counted(value = "moo.search.users", description = "Number of user search requests")
  @Operation(summary = "Search users")
  @ApiResponse(responseCode = "200", description = "Page of matching users")
  public ResponseEntity<Page<UserSearchModel>> searchUsers(
      @RequestParam("q") String query,
      @PageableDefault(
              size = 20,
              sort = {CREATED_AT, ID},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    Utility.validatePageable(pageable, CREATED_AT, ID, "followerCount", "name", "username");
    Page<UserSearchModel> users = searchService.searchUsers(query, ensureIdSort(pageable));
    return new ResponseEntity<>(users, HttpStatus.OK);
  }

  private Pageable ensureIdSort(Pageable pageable) {
    if (!pageable.isPaged() || pageable.getSort().getOrderFor(ID) != null) {
      return pageable;
    }
    Sort tieBreaker = Sort.by(Sort.Direction.DESC, ID);
    Sort combinedSort =
        pageable.getSort().isSorted() ? pageable.getSort().and(tieBreaker) : tieBreaker;
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), combinedSort);
  }
}
