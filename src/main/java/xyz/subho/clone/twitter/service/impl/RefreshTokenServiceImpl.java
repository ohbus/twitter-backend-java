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

package xyz.subho.clone.twitter.service.impl;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.subho.clone.twitter.entity.RefreshToken;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.exception.ResourceNotFoundException;
import xyz.subho.clone.twitter.repository.RefreshTokenRepository;
import xyz.subho.clone.twitter.repository.UsersRepository;
import xyz.subho.clone.twitter.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

  @Value("${jwt.refreshExpiration}")
  private Long refreshTokenDurationMs;

  private final RefreshTokenRepository refreshTokenRepository;
  private final UsersRepository usersRepository;

  @PostConstruct
  void validateConfiguration() {
    if (refreshTokenDurationMs == null || refreshTokenDurationMs <= 0) {
      throw new IllegalStateException("jwt.refreshExpiration must be positive");
    }
  }

  public RefreshTokenServiceImpl(
      RefreshTokenRepository refreshTokenRepository, UsersRepository usersRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.usersRepository = usersRepository;
  }

  @Override
  @Transactional
  public RefreshToken createRefreshToken(@NonNull UUID userId) {
    var user =
        usersRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User is Invalid"));

    return createRefreshTokenForUser(user);
  }

  private RefreshToken createRefreshTokenForUser(Users user) {
    refreshTokenRepository.deleteByUsers(user);
    refreshTokenRepository.flush();

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUsers(user);
    refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
    String rawToken = UUID.randomUUID().toString();
    refreshToken.setToken(hashToken(rawToken));
    refreshToken.setRawToken(rawToken);

    return refreshTokenRepository.save(refreshToken);
  }

  private RefreshToken verifyExpiration(@NonNull RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(token);
      throw new BadRequestException("Refresh token was expired. Please make a new signin request");
    }
    return token;
  }

  @Override
  @Transactional
  public RefreshToken rotateRefreshToken(@NonNull String tokenValue) {
    var currentToken =
        refreshTokenRepository
            .findByTokenForUpdate(hashToken(tokenValue))
            .orElseThrow(() -> new BadRequestException("Refresh token is not in database!"));
    verifyExpiration(currentToken);
    var user =
        usersRepository
            .findByIdForUpdate(currentToken.getUsers().getId())
            .orElseThrow(() -> new ResourceNotFoundException("User is Invalid"));
    return createRefreshTokenForUser(user);
  }

  @Override
  @Transactional
  public int deleteByUserId(@NonNull UUID userId) {
    var user = usersRepository.findByIdForUpdate(userId).orElse(null);
    return user == null ? 0 : refreshTokenRepository.deleteByUsers(user);
  }

  private String hashToken(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
