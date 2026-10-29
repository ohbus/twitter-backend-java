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

package xyz.subho.clone.twitter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import xyz.subho.clone.twitter.entity.HashtagPosts;
import xyz.subho.clone.twitter.entity.Hashtags;
import xyz.subho.clone.twitter.entity.Likes;
import xyz.subho.clone.twitter.entity.Posts;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.repository.HashtagPostsRepository;
import xyz.subho.clone.twitter.repository.HashtagsRepository;
import xyz.subho.clone.twitter.repository.LikesRepository;
import xyz.subho.clone.twitter.repository.PostsRepository;
import xyz.subho.clone.twitter.repository.UsersRepository;
import xyz.subho.clone.twitter.service.PostService;
import xyz.subho.clone.twitter.service.SearchService;

@SpringBootTest
@ActiveProfiles("test")
class TwitterBackendApplicationTests {

  @Autowired private HashtagPostsRepository hashtagPostsRepository;
  @Autowired private HashtagsRepository hashtagsRepository;
  @Autowired private LikesRepository likesRepository;
  @Autowired private PostsRepository postsRepository;
  @Autowired private PostService postService;
  @Autowired private SearchService searchService;
  @Autowired private UsersRepository usersRepository;

  @Test
  void contextLoads() {}

  @Test
  @Transactional
  void searchFindsUsersAndPostsCaseInsensitively() {
    Users user = user("search-dev", "search-dev@example.com");
    user.setBio("Passionate Spring Boot and Java Engineer");
    usersRepository.saveAndFlush(user);

    Posts post = new Posts();
    post.setText("Hello world, this is a special #SpringFramework update!");
    post.setUsers(user);
    postsRepository.saveAndFlush(post);

    var userResults = searchService.searchUsers("spring boot", PageRequest.of(0, 10));
    assertThat(userResults.getContent()).extracting("username").contains("search-dev");

    var userHandleResults = searchService.searchUsers("@search-dev", PageRequest.of(0, 10));
    assertThat(userHandleResults.getContent()).extracting("username").contains("search-dev");

    var postResults = searchService.searchPosts("SPRINGFRAMEWORK", PageRequest.of(0, 10));
    assertThat(postResults.getContent()).extracting("text").contains(post.getText());
  }

  @Test
  @Transactional
  void deletingAPostCanDecrementItsPersistedHashtagCount() {
    Users user = new Users();
    user.setUsername("hashtag-test-user");
    user.setName("Hashtag Test User");
    user.setEmail("hashtag-test@example.com");
    user.setPassword("hashed-password");
    usersRepository.saveAndFlush(user);

    Posts post = new Posts();
    post.setText("post");
    post.setUsers(user);
    postsRepository.saveAndFlush(post);

    Hashtags hashtag = new Hashtags();
    hashtag.setTag("hashtag-test");
    hashtag.setRecentPostCount(1);
    hashtagsRepository.saveAndFlush(hashtag);

    HashtagPosts join = new HashtagPosts();
    join.setPosts(post);
    join.setHashtags(hashtag);
    hashtagPostsRepository.saveAndFlush(join);

    assertThat(hashtagsRepository.decrementRecentPostCountForPost(post.getId())).isEqualTo(1);
    assertThat(hashtagsRepository.findById(hashtag.getId()).orElseThrow().getRecentPostCount())
        .isZero();
  }

  @Test
  @Transactional
  void deletingAPostRemovesJoinsAndClearsReferences() {
    Users owner = user("delete-owner", "delete-owner@example.com");
    Users liker = user("delete-liker", "delete-liker@example.com");
    usersRepository.saveAllAndFlush(List.of(owner, liker));

    Posts post = new Posts();
    post.setText("post");
    post.setUsers(owner);
    postsRepository.saveAndFlush(post);

    Posts originalPost = new Posts();
    originalPost.setText("original");
    originalPost.setUsers(owner);
    postsRepository.saveAndFlush(originalPost);

    Posts reply = new Posts();
    reply.setText("reply");
    reply.setUsers(liker);
    reply.setReplyToId(post.getId());
    reply.setOriginalPostId(originalPost.getId());
    postsRepository.saveAndFlush(reply);

    Hashtags hashtag = new Hashtags();
    hashtag.setTag("delete-test");
    hashtag.setRecentPostCount(1);
    hashtagsRepository.saveAndFlush(hashtag);

    HashtagPosts join = new HashtagPosts();
    join.setPosts(post);
    join.setHashtags(hashtag);
    hashtagPostsRepository.saveAndFlush(join);

    Likes like = new Likes();
    like.setPosts(post);
    like.setUsers(liker);
    likesRepository.saveAndFlush(like);

    postService.deletePost(post.getId(), owner.getId());

    assertThat(postsRepository.findById(post.getId())).isEmpty();
    assertThat(hashtagPostsRepository.findById(join.getId())).isEmpty();
    assertThat(likesRepository.findById(like.getId())).isEmpty();
    assertThat(postsRepository.findById(reply.getId()).orElseThrow().getReplyToId()).isNull();
    assertThat(postsRepository.findById(reply.getId()).orElseThrow().getOriginalPostId())
        .isEqualTo(originalPost.getId());
    assertThat(hashtagsRepository.findById(hashtag.getId()).orElseThrow().getRecentPostCount())
        .isZero();
  }

  private Users user(String username, String email) {
    Users user = new Users();
    user.setUsername(username);
    user.setName(username);
    user.setEmail(email);
    user.setPassword("hashed-password");
    return user;
  }
}
