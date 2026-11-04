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

package xyz.subho.clone.twitter.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "users",
    indexes = {
      @Index(name = "idx_users_name", columnList = "name"),
      @Index(name = "idx_users_bio", columnList = "bio")
    })
public class Users extends Auditable {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(unique = true, nullable = false, length = 30)
  private String username;

  @Column(nullable = false, length = 50)
  private String name;

  private String avatar;

  @Column(unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(length = 240)
  private String bio;

  @Column(name = "follower_count", nullable = false)
  private long followerCount = 0L;

  @Column(name = "following_count", nullable = false)
  private Long followingCount = 0L;

  @Column(nullable = false)
  private Boolean verified = false;

  @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Likes> userLikes = new ArrayList<>();

  @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Follow> followers = new ArrayList<>();

  @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Follow> following = new ArrayList<>();

  @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Posts> userPosts = new ArrayList<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public long getFollowerCount() {
    return followerCount;
  }

  public void setFollowerCount(long followerCount) {
    this.followerCount = followerCount;
  }

  public Long getFollowingCount() {
    return followingCount;
  }

  public void setFollowingCount(Long followingCount) {
    this.followingCount = followingCount;
  }

  public Boolean getVerified() {
    return verified;
  }

  public void setVerified(Boolean verified) {
    this.verified = verified;
  }

  public List<Likes> getUserLikes() {
    return userLikes;
  }

  public void setUserLikes(List<Likes> userLikes) {
    this.userLikes = userLikes;
  }

  public List<Follow> getFollowers() {
    return followers;
  }

  public void setFollowers(List<Follow> followers) {
    this.followers = followers;
  }

  public List<Follow> getFollowing() {
    return following;
  }

  public void setFollowing(List<Follow> following) {
    this.following = following;
  }

  public List<Posts> getUserPosts() {
    return userPosts;
  }

  public void setUserPosts(List<Posts> userPosts) {
    this.userPosts = userPosts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Users users = (Users) o;
    return Objects.equals(id, users.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "Users{"
        + "id="
        + id
        + ", username='"
        + username
        + '\''
        + ", name='"
        + name
        + '\''
        + '}';
  }
}
