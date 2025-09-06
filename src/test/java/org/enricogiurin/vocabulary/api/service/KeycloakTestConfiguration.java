package org.enricogiurin.vocabulary.api.service;

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

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.enricogiurin.vocabulary.api.conf.KeycloakConfig;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class KeycloakTestConfiguration {

  private static final String TEST_REALM_JSON = "keycloak/test-realm.json";

  @Bean
  KeycloakContainer keycloakContainer() {
    return new KeycloakContainer()
        .withAdminUsername("admin")
        .withAdminPassword("Pa55w0rd")
        .withRealmImportFiles(TEST_REALM_JSON);
  }

  @Primary
  @Bean("keycloakTest")
  Keycloak keycloak(KeycloakContainer container,
      @Value("${application.keycloak.username}") final String username,
      @Value("${application.keycloak.password}") final String password) {
    return KeycloakBuilder.builder()
        .serverUrl(container.getAuthServerUrl())
        .realm(KeycloakConfig.REALM)
        .clientId(KeycloakConfig.CLIENT_ID)
        .grantType(OAuth2Constants.PASSWORD)
        .username(username)
        .password(password)
        .build();
  }

}
