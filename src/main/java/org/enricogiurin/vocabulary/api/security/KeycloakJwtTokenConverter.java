package org.enricogiurin.vocabulary.api.security;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 - 2025 Vocabulary Team
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class KeycloakJwtTokenConverter implements Converter<Jwt, JwtAuthenticationToken> {

  private static final String RESOURCE_ACCESS = "resource_access";
  private static final String ROLES = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  private final String clientId;

  KeycloakJwtTokenConverter(@Value("${application.keycloak.client-id}") String clientId) {
    this.clientId = clientId;
  }


  @Override
  public JwtAuthenticationToken convert(Jwt jwt) {
    String tokenValue = jwt.getTokenValue();
    log.debug("token: {}", tokenValue);
    Collection<GrantedAuthority> authorities = extractResourceRoles(jwt);
    return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
  }

  private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
    Object resourceAccess = jwt.getClaims().get(RESOURCE_ACCESS);
    Collection<GrantedAuthority> result = Collections.emptySet();
    if (resourceAccess instanceof Map<?, ?> map) {
      Object client = map.get(clientId);
      if (client instanceof Map<?, ?> clientMap) {
        Object roles = clientMap.get(ROLES);
        if (roles instanceof List<?> roleList) {
          result = roleList.stream()
              .filter(String.class::isInstance)
              .map(String.class::cast)
              .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
              .collect(Collectors.toSet());
        }
      }
    }
    return result;
  }
}
