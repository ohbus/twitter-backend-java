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

package xyz.subho.clone.twitter.constant;

/** AuthV1Constants - API endpoint constants for Authentication V1 Controller. */
public class AuthV1Constants {

  public static final String BASE_PATH = ApiVersion.V1;
  public static final String AUTHENTICATE = "/authenticate";
  public static final String REFRESH = "/refresh";
  public static final String LOGOUT = "/logout";
  public static final String CSRF = "/csrf";

  private AuthV1Constants() {
    // Prevent instantiation
  }
}
