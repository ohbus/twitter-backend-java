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

package xyz.subho.clone.twitter.utility;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.model.UserModel;
import xyz.subho.clone.twitter.model.UserSearchModel;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "password", ignore = true)
  UserModel toModel(Users user);

  UserSearchModel toSearchModel(Users user);

  @Mapping(target = "userLikes", ignore = true)
  @Mapping(target = "userPosts", ignore = true)
  @Mapping(target = "followers", ignore = true)
  @Mapping(target = "following", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "followerCount", ignore = true)
  @Mapping(target = "followingCount", ignore = true)
  @Mapping(target = "verified", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Users toEntity(UserModel userModel);
}
