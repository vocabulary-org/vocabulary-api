package org.enricogiurin.vocabulary.api.conf;

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

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

  public static final String REALM = "master";
  public static final String CLIENT_ID = "admin-cli";

  @Bean
  Keycloak keycloak(@Value("${application.keycloak.url}") String keycloakUrl,
      @Value("${application.keycloak.username}") String username,
      @Value("${application.keycloak.password}") String password
  ) {
    return KeycloakBuilder.builder()
        .serverUrl(keycloakUrl)
        .realm(REALM)
        .clientId(CLIENT_ID)
        .grantType(OAuth2Constants.PASSWORD)
        .username(username)
        .password(password)
        .build();
  }

}
