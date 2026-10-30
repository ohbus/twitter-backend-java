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

package xyz.subho.clone.twitter.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PostModel(
    @Nullable UUID id,
    @NotBlank(message = "Post text cannot be empty")
        @Size(max = 240, message = "Post text cannot exceed 240 characters")
        String text,
    @Nullable UUID userId,
    @Size(max = 4) List<@NotBlank @Size(max = 255) String> images,
    @Nullable Long likeCount,
    @Nullable Long repostCount,
    @Nullable UUID originalPostId,
    @Nullable UUID replyToId,
    @Nullable Instant timestamp,
    @Size(max = 20) List<@NotBlank @Size(max = 50) String> hashtags,
    @Size(max = 20) List<@NotBlank @Size(max = 50) String> mentions) {

  public PostModel {
    if (images == null) images = new ArrayList<>(4);
    if (hashtags == null) hashtags = new ArrayList<>();
    if (mentions == null) mentions = new ArrayList<>();
  }
}
