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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.subho.clone.twitter.entity.Users;

public interface UsersRepository extends JpaRepository<Users, UUID> {

  public Users findByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from Users u where u.username = :username")
  Users findByUsernameForUpdate(@Param("username") String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from Users u where u.id = :id")
  Optional<Users> findByIdForUpdate(@Param("id") UUID id);

  @Query(
      "select u from Users u where "
          + "lower(u.username) like lower(concat('%', :query, '%')) escape '\\' or "
          + "lower(u.name) like lower(concat('%', :query, '%')) escape '\\' or "
          + "(u.bio is not null and lower(u.bio) like lower(concat('%', :query, '%')) escape '\\')")
  @NonNull Page<Users> searchUsers(
      @Param("query") @NonNull String query, @NonNull Pageable pageable);
}
