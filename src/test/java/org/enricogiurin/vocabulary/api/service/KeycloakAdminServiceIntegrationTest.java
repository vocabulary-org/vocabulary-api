package org.enricogiurin.vocabulary.api.service;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
  public static final KeycloakContainer KEYCLOAK = new KeycloakContainer()
      .withRealmImportFile(TEST_REALM_JSON)
      // this would normally be just "target/classes"
      .withProviderClassesFrom("target/test-classes")
      // this enables KeycloakContainer reuse across tests
      .withReuse(true);
  @Autowired
  Keycloak keycloak;

  @BeforeAll
  public static void beforeAll() {
    KEYCLOAK.start();
  }

  @AfterAll
  public static void afterAll() {
    KEYCLOAK.stop();
  }

  @Test
  void test() {
  }

}
