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

package xyz.subho.clone.twitter.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private Long expiration;

  @PostConstruct
  void validateConfiguration() {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("jwt.secret must contain at least 32 UTF-8 bytes");
    }
    if (expiration == null || expiration <= 0) {
      throw new IllegalStateException("jwt.expiration must be positive");
    }
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Instant extractExpiration(String token) {
    var expiration = extractClaim(token, Claims::getExpiration);
    return expiration == null ? null : expiration.toInstant();
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
  }

  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    return createToken(claims, userDetails.getUsername());
  }

  @SuppressWarnings("java:S2143") // JJWT 0.12 accepts java.util.Date at this library boundary.
  private String createToken(Map<String, Object> claims, String subject) {
    Instant issuedAt = Instant.now();
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(java.util.Date.from(issuedAt))
        .expiration(java.util.Date.from(issuedAt.plusMillis(expiration)))
        .signWith(getSignInKey())
        .compact();
  }

  public Boolean validateToken(String token, UserDetails userDetails) {
    String username = validateAndExtractUsername(token);
    return username != null && username.equals(userDetails.getUsername());
  }

  public boolean validateToken(String token) {
    return validateAndExtractUsername(token) != null;
  }

  String validateAndExtractUsername(String token) {
    try {
      Claims claims = extractAllClaims(token);
      String username = claims.getSubject();
      var tokenExpiration = claims.getExpiration();
      return username != null && tokenExpiration != null ? username : null;
    } catch (JwtException | IllegalArgumentException _) {
      return null;
    }
  }

  private SecretKey getSignInKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
}
