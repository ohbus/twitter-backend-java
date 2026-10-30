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

package xyz.subho.clone.twitter.utility;

import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import xyz.subho.clone.twitter.exception.BadRequestException;

@Component
public class Utility {

  // shortcut: page-based pagination stops at 10,000 rows; replace with cursors for deeper feeds.
  private static final long MAX_PAGE_OFFSET = 10_000L;

  public static void validatePageable(Pageable pageable, String... allowedSortProperties) {
    if (pageable.getOffset() > MAX_PAGE_OFFSET) {
      throw new BadRequestException("Page is too deep; use a smaller page number");
    }
    Set<String> allowedProperties = Set.of(allowedSortProperties);
    pageable
        .getSort()
        .forEach(
            order -> {
              if (!allowedProperties.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort property: " + order.getProperty());
              }
            });
  }

  /**
   * @param String uuid
   * @return TRUE or FALSE if the given String is a valid UUID
   */
  public boolean isUUID(String uuid) {
    return converStringToUUID(uuid) != null;
  }

  /**
   * @param String uuid
   * @return UUID uuid or NULL if invalid
   */
  public UUID converStringToUUID(String uuid) {
    if (uuid == null) {
      return null;
    }
    try {
      return UUID.fromString(uuid);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }
}
