/*
 * Twitter Backend - Moo: Twitter Clone Application Backend by Scaler
 * Copyright © 2021-2026 Subhrodip Mohanta (hello@subho.xyz)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package xyz.subho.clone.twitter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import xyz.subho.clone.twitter.entity.Users;
import xyz.subho.clone.twitter.repository.UsersRepository;

class UserDetailsServiceTest {

  @Test
  void loadsStoredCredentials() {
    UsersRepository repository = mock(UsersRepository.class);
    Users user = new Users();
    user.setUsername("alice");
    user.setPassword("hash");
    when(repository.findByUsername("alice")).thenReturn(user);

    var details = new UserDetailsServiceImpl(repository).loadUserByUsername("alice");
    assertThat(details.getUsername()).isEqualTo("alice");
    assertThat(details.getPassword()).isEqualTo("hash");
  }

  @Test
  void missingUserIsRejected() {
    UsersRepository repository = mock(UsersRepository.class);
    when(repository.findByUsername("missing")).thenReturn(null);
    UserDetailsServiceImpl service = new UserDetailsServiceImpl(repository);

    assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
  }
}
