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
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
@Testcontainers
class KeycloakAdminServiceIntegrationTest {

  private static final String TEST_REALM_JSON = "keycloak/test-realm.json";

  @Container
  static KeycloakContainer keycloak = new KeycloakContainer()
      .withAdminUsername("admin")
      .withAdminPassword("Pa55w0rd")
      .withRealmImportFiles(TEST_REALM_JSON);
  @Autowired
  KeycloakAdminService keycloakAdminService;

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("application.keycloak.url", keycloak::getAuthServerUrl);
  }

  @Test
  void printUsers() {
    keycloakAdminService.printUsers();
  }



}
