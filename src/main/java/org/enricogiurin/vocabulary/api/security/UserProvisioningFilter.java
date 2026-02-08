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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.enricogiurin.vocabulary.api.service.UserService;
import org.enricogiurin.vocabulary.api.service.UserService.UserCreationAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component

public class UserProvisioningFilter extends OncePerRequestFilter {

  private final PrincipalAccessor principalAccessor;

  private final UserService userService;
  private final CurrentUser currentUser;
  private final String publicPath;

  public UserProvisioningFilter(PrincipalAccessor principalAccessor, UserService userService,
      CurrentUser currentUser,
      @Value("${application.api.public-path}") String publicPath) {
    super();
    this.principalAccessor = principalAccessor;
    this.userService = userService;
    this.currentUser = currentUser;
    this.publicPath = publicPath;
  }


  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith(publicPath) ||
        path.startsWith("/actuator")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String subject = principalAccessor.getSubject();
    if (subject != null) {
      UserCreationAttributes userCreationAttributes = new UserCreationAttributes(
          principalAccessor.getUsername());
      Integer userIdByKeycloakId = userService.findOrCreateUserIdByKeycloakId(subject,
          userCreationAttributes);
      currentUser.setUserId(userIdByKeycloakId);
    }
    filterChain.doFilter(request, response);
  }

}
