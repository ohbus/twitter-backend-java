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

package xyz.subho.clone.twitter.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.subho.clone.twitter.entity.Posts;

public interface PostsRepository extends JpaRepository<Posts, UUID> {

  public Page<Posts> findByReplyToId(UUID replyToId, Pageable pageable);

  @Modifying(clearAutomatically = false, flushAutomatically = true)
  @Query(
      "update Posts p set p.replyToId = case when p.replyToId = :postId then null else p.replyToId end, "
          + "p.originalPostId = case when p.originalPostId = :postId then null else p.originalPostId end "
          + "where p.replyToId = :postId or p.originalPostId = :postId")
  int clearReferences(@Param("postId") UUID postId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Posts p where p.id = :id")
  Optional<Posts> findByIdForUpdate(@Param("id") UUID id);

  @Query(
      "select p from Posts p where "
          + "(p.text is not null and lower(p.text) like lower(concat('%', :query, '%')) escape '\\')")
  @NonNull Page<Posts> searchPosts(
      @Param("query") @NonNull String query, @NonNull Pageable pageable);
}
