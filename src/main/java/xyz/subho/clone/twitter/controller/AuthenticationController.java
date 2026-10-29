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

package xyz.subho.clone.twitter.controller;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.subho.clone.twitter.constant.AuthV1Constants;
import xyz.subho.clone.twitter.exception.BadRequestException;
import xyz.subho.clone.twitter.model.AuthenticationRequest;
import xyz.subho.clone.twitter.model.AuthenticationResponse;
import xyz.subho.clone.twitter.model.TokenRefreshRequest;
import xyz.subho.clone.twitter.security.JwtUtil;
import xyz.subho.clone.twitter.security.UserDetailsServiceImpl;
import xyz.subho.clone.twitter.service.RefreshTokenService;
import xyz.subho.clone.twitter.service.UserService;

@RestController
@RequestMapping(AuthV1Constants.BASE_PATH)
@Timed(value = "moo.auth.timer", description = "Time taken to process auth requests")
public class AuthenticationController {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtTokenUtil;
  private final UserDetailsServiceImpl userDetailsService;
  private final RefreshTokenService refreshTokenService;
  private final UserService userService;

  public AuthenticationController(
      AuthenticationManager authenticationManager,
      JwtUtil jwtTokenUtil,
      UserDetailsServiceImpl userDetailsService,
      RefreshTokenService refreshTokenService,
      UserService userService) {
    this.authenticationManager = authenticationManager;
    this.jwtTokenUtil = jwtTokenUtil;
    this.userDetailsService = userDetailsService;
    this.refreshTokenService = refreshTokenService;
    this.userService = userService;
  }

  @PostMapping(AuthV1Constants.AUTHENTICATE)
  @Counted(value = "moo.auth.login", description = "Number of user logins")
  public ResponseEntity<AuthenticationResponse> createAuthenticationToken(
      @Valid @RequestBody AuthenticationRequest authenticationRequest) {

    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              authenticationRequest.username(), authenticationRequest.password()));
    } catch (BadCredentialsException e) {
      throw new BadRequestException("Incorrect username or password", e);
    }

    final var userDetails = userDetailsService.loadUserByUsername(authenticationRequest.username());

    final String jwt = jwtTokenUtil.generateToken(userDetails);
    var user = userService.getUserByUserName(authenticationRequest.username());
    if (user == null) {
      throw new BadRequestException("Incorrect username or password");
    }
    var refreshToken = refreshTokenService.createRefreshToken(Objects.requireNonNull(user.id()));

    return ResponseEntity.ok(new AuthenticationResponse(jwt, refreshToken.getToken()));
  }

  @GetMapping(AuthV1Constants.CSRF)
  public CsrfToken csrfToken(CsrfToken csrfToken) {
    return csrfToken;
  }

  @PostMapping(AuthV1Constants.REFRESH)
  public ResponseEntity<AuthenticationResponse> refreshToken(
      @Valid @RequestBody TokenRefreshRequest request) {
    var token = refreshTokenService.rotateRefreshToken(request.refreshToken());
    var user = token.getUsers();
    String jwt =
        jwtTokenUtil.generateToken(userDetailsService.loadUserByUsername(user.getUsername()));
    return ResponseEntity.ok(new AuthenticationResponse(jwt, token.getToken()));
  }

  @PostMapping(AuthV1Constants.LOGOUT)
  public ResponseEntity<String> logoutUser(Principal principal) {
    var user = userService.getUserByUserName(principal.getName());
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    refreshTokenService.deleteByUserId(Objects.requireNonNull(user.id()));
    return ResponseEntity.ok("Log out successful!");
  }
}
