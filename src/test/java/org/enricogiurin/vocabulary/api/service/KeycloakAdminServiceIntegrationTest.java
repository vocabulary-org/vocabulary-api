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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class KeycloakAdminServiceIntegrationTest {

  private static final String TEST_REALM_JSON = "keycloak/test-realm.json";
  //private static final String TEST_REALM_JSON = "keycloak/vocabulary-realm.json";
/*  public static final KeycloakContainer KEYCLOAK = new KeycloakContainer()
      .withRealmImportFile(TEST_REALM_JSON)
      // this would normally be just "target/classes"
      .withProviderClassesFrom("target/test-classes")
      // this enables KeycloakContainer reuse across tests
      .withReuse(true);*/
  @Autowired
  KeycloakAdminService keycloakAdminService;

  //@BeforeAll
/*  public static void beforeAll() {
    KEYCLOAK.start();
  }*/

  //@AfterAll
/*  public static void afterAll() {
    KEYCLOAK.stop();
  }*/

  @Test
  @Disabled
  void test() {
    keycloakAdminService.printUsers();
  }



}
