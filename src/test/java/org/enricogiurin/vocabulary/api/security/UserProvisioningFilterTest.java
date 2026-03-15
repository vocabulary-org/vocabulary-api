package org.enricogiurin.vocabulary.api.security;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 - 2026 Vocabulary Team
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.enricogiurin.vocabulary.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@TestPropertySource(locations = "classpath:application.yml")
class UserProvisioningFilterTest {

  @Value("${application.api.public-path}")
  String publicPath;

  @Value("${application.api.user-path}")
  String userPath;

  @Mock PrincipalAccessor principalAccessor;
  @Mock UserService userService;
  @Mock CurrentUser currentUser;
  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock FilterChain filterChain;

  UserProvisioningFilter filter;

  @BeforeEach
  void setUp() {
    filter = new UserProvisioningFilter(principalAccessor, userService, currentUser, publicPath);
  }

  @Test
  void doFilter_skipsProvisioningWhenPrincipalIsNotJwt() throws Exception {
    when(request.getRequestURI()).thenReturn(userPath + "/users");
    when(principalAccessor.isValid()).thenReturn(false);

    filter.doFilter(request, response, filterChain);

    verify(principalAccessor, never()).getSubject();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_provisionsUserWhenPrincipalIsJwt() throws Exception {
    when(request.getRequestURI()).thenReturn(userPath + "/users");
    when(principalAccessor.isValid()).thenReturn(true);
    when(principalAccessor.getSubject()).thenReturn("some-keycloak-id");
    when(principalAccessor.getUsername()).thenReturn("john");
    when(userService.findOrSaveUserIdByKeycloakId(any(), any())).thenReturn(42);

    filter.doFilter(request, response, filterChain);

    verify(userService).findOrSaveUserIdByKeycloakId(any(), any());
    verify(currentUser).setUserId(42);
    verify(currentUser).setSubject("some-keycloak-id");
    verify(filterChain).doFilter(request, response);
  }
}
