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
    user.setName("Spring Boot Developer");
    user.setBio("Spring Boot and Java Engineer");
    usersRepository.saveAndFlush(user);

    Posts post = post(user, "SpringFramework update: Hello world!");
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
  void searchPostsEscapedWildcardsMatchesOnlyLiteralRowsAgainstDatabase() {
    Users author = user("wildcard-author", "wildcard-author@example.com");
    usersRepository.saveAndFlush(author);

    // Literal % and near-matching row
    Posts postLiteralPercent = post(author, "100% discount deal");
    Posts postNearPercent = post(author, "1000 discount deal");

    // Literal _ and near-matching row
    Posts postLiteralUnderscore = post(author, "v1_release update");
    Posts postNearUnderscore = post(author, "v1arelease update");

    // Literal \ and near-matching row
    Posts postLiteralBackslash = post(author, "C:\\Windows\\System32");
    Posts postNearBackslash = post(author, "C:WindowsSystem32");

    postsRepository.saveAllAndFlush(
        List.of(
            postLiteralPercent,
            postNearPercent,
            postLiteralUnderscore,
            postNearUnderscore,
            postLiteralBackslash,
            postNearBackslash));

    var percentResults = searchService.searchPosts("100%", PageRequest.of(0, 10));
    assertThat(percentResults.getContent())
        .extracting("text")
        .containsExactly("100% discount deal");

    var underscoreResults = searchService.searchPosts("v1_", PageRequest.of(0, 10));
    assertThat(underscoreResults.getContent())
        .extracting("text")
        .containsExactly("v1_release update");

    var backslashResults = searchService.searchPosts("C:\\", PageRequest.of(0, 10));
    assertThat(backslashResults.getContent())
        .extracting("text")
        .containsExactly("C:\\Windows\\System32");
  }

  @Test
  @Transactional
  void searchUsersEscapedWildcardsMatchesOnlyLiteralRowsAgainstDatabase() {
    // Literal % and near-matching user
    Users userLiteralPercent = user("alice%dev", "alice.percent@example.com");
    userLiteralPercent.setName("Alice%Dev");
    Users userNearPercent = user("alice1dev", "alice.one@example.com");
    userNearPercent.setName("Alice1Dev");

    // Literal _ and near-matching user
    Users userLiteralUnderscore = user("bob_coder", "bob.under@example.com");
    userLiteralUnderscore.setName("Bob_Coder");
    Users userNearUnderscore = user("bob1coder", "bob.one@example.com");
    userNearUnderscore.setName("Bob1Coder");

    // Literal \ and near-matching user
    Users userLiteralBackslash = user("charlie\\sec", "charlie.slash@example.com");
    userLiteralBackslash.setName("Charlie\\Sec");
    Users userNearBackslash = user("charliesec", "charlie.sec@example.com");
    userNearBackslash.setName("CharlieSec");

    usersRepository.saveAllAndFlush(
        List.of(
            userLiteralPercent,
            userNearPercent,
            userLiteralUnderscore,
            userNearUnderscore,
            userLiteralBackslash,
            userNearBackslash));

    var percentResults = searchService.searchUsers("alice%", PageRequest.of(0, 10));
    assertThat(percentResults.getContent()).extracting("username").containsExactly("alice%dev");

    var underscoreResults = searchService.searchUsers("bob_", PageRequest.of(0, 10));
    assertThat(underscoreResults.getContent()).extracting("username").containsExactly("bob_coder");

    var backslashResults = searchService.searchUsers("charlie\\", PageRequest.of(0, 10));
    assertThat(backslashResults.getContent())
        .extracting("username")
        .containsExactly("charlie\\sec");
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

  private Posts post(Users user, String text) {
    Posts post = new Posts();
    post.setText(text);
    post.setUsers(user);
    return post;
  }
}
