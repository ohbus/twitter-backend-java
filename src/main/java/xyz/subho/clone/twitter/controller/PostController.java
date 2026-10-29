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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.subho.clone.twitter.constant.PostV1Constants;
import xyz.subho.clone.twitter.model.PostModel;
import xyz.subho.clone.twitter.service.PostService;
import xyz.subho.clone.twitter.service.UserService;
import xyz.subho.clone.twitter.utility.Utility;

@RestController
@RequestMapping(PostV1Constants.BASE_PATH)
@Timed(value = "moo.posts.timer", description = "Time taken to process post requests")
public class PostController {

  private final PostService postService;
  private final UserService userService;

  public PostController(PostService postService, UserService userService) {
    this.postService = postService;
    this.userService = userService;
  }

  @GetMapping
  public ResponseEntity<Page<PostModel>> getAllPosts(
      @PageableDefault(
              size = 20,
              sort = {"createdAt", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    Utility.validatePageable(pageable, "createdAt", "id");
    Page<PostModel> posts = postService.getAllPosts(pageable);
    return new ResponseEntity<>(posts, HttpStatus.OK);
  }

  @GetMapping(PostV1Constants.POST_ID)
  public ResponseEntity<PostModel> getPost(@PathVariable("postId") UUID postId) {
    PostModel post = postService.getPost(postId);
    return new ResponseEntity<>(post, HttpStatus.OK);
  }

  @PostMapping
  @Counted(value = "moo.posts.created", description = "Number of posts created")
  public ResponseEntity<PostModel> addPost(
      @Valid @RequestBody PostModel postModel, Principal principal) {
    var user = userService.getUserByUserName(principal.getName());
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    // Create new record instance with the authenticated userId
    PostModel enrichedPost =
        new PostModel(
            null,
            postModel.text(),
            Objects.requireNonNull(user.id()),
            postModel.images(),
            null,
            null,
            postModel.originalPostId(),
            postModel.replyToId(),
            null,
            postModel.hashtags(),
            postModel.mentions());

    PostModel post = postService.addPost(enrichedPost);
    return new ResponseEntity<>(post, HttpStatus.CREATED);
  }

  @DeleteMapping(PostV1Constants.POST_ID)
  public ResponseEntity<HttpStatus> deletePost(
      @PathVariable("postId") UUID postId, Principal principal) {

    var user = userService.getUserByUserName(principal.getName());
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    postService.deletePost(postId, Objects.requireNonNull(user.id()));
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PutMapping(PostV1Constants.LIKE)
  @Counted(value = "moo.posts.liked", description = "Number of posts liked")
  public ResponseEntity<Long> likePost(@PathVariable("postId") UUID postId, Principal principal) {

    var user = userService.getUserByUserName(principal.getName());
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(
        postService.addLike(postId, Objects.requireNonNull(user.id())), HttpStatus.CREATED);
  }

  @DeleteMapping(PostV1Constants.LIKE)
  public ResponseEntity<Long> removeLikePost(
      @PathVariable("postId") UUID postId, Principal principal) {

    var user = userService.getUserByUserName(principal.getName());
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(
        postService.removeLike(postId, Objects.requireNonNull(user.id())), HttpStatus.OK);
  }

  @GetMapping(PostV1Constants.REPLIES)
  public ResponseEntity<Page<PostModel>> getReplies(
      @PathVariable("postId") UUID postId,
      @PageableDefault(
              size = 20,
              sort = {"createdAt", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    Utility.validatePageable(pageable, "createdAt", "id");
    return new ResponseEntity<>(postService.getReplies(postId, pageable), HttpStatus.OK);
  }
}
