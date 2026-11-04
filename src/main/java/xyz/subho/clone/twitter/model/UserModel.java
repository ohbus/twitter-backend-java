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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record UserModel(
    @Nullable UUID id,
    @NotBlank(message = "Username is mandatory")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        String username,
    @NotBlank(message = "Name is mandatory")
        @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
        String name,
    @NotBlank(message = "Password is mandatory")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
        String password,
    @NotBlank(message = "Email is mandatory")
        @Email(message = "Email should be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,
    @Nullable @Size(max = 240) String avatar,
    @Nullable @Size(max = 240) String bio,
    @Nullable Long followerCount,
    @Nullable Long followingCount,
    @Nullable Boolean verified) {}
