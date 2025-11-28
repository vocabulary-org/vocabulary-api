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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.exception.DataConflictException;
import org.enricogiurin.vocabulary.api.model.KeycloakUser;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Import({VocabularyTestConfiguration.class})
@Transactional
class KeycloakClientServiceTest {

  private static final String USER_EMAIL = "john.doe@example.com";
  private static final String TEST_REALM_JSON = "keycloak/test-realm.json";
  static final KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer()
      .withAdminUsername("admin")
      .withAdminPassword("pwd")
      .withRealmImportFiles(TEST_REALM_JSON)
      .withReuse(true);

  @Autowired
  UserRepository userRepository;

  KeycloakClientService keycloakClientService;

  @BeforeAll
  public static void beforeAll() {
    KEYCLOAK_CONTAINER.start();
  }

  @AfterAll
  public static void afterAll() {
    KEYCLOAK_CONTAINER.stop();
  }

  @BeforeEach
  void setUp() {
    Keycloak keycloakAdminClient = KEYCLOAK_CONTAINER.getKeycloakAdminClient();
    this.keycloakClientService = new KeycloakClientService(keycloakAdminClient, userRepository, "",
        true);
  }


  @Test
  void userList() {
    //when
    List<UserRepresentation> userRepresentationList = keycloakClientService.userList();
    //then
    assertThat(userRepresentationList).isNotNull();
    assertThat(userRepresentationList)
        .singleElement()
        .extracting(UserRepresentation::getUsername)
        .isEqualTo("test-user");
  }

  //to evaluate also if user is present in the KC test container
  @Test
  void createNewUser() {
    //given
    KeycloakUser user = KeycloakUser.builder()
        .username("johndoe")
        .firstName("John")
        .lastName("Doe")
        .email(USER_EMAIL)
        .isAdmin(false)
        .build();
    //when
    String keycloakId = keycloakClientService.createNewUser(user);

    //then
    userRepository.findUserIdByKeycloakId(keycloakId).orElseThrow();
  }

  @Test
  void createNewUser_conflict() {
    //given
    KeycloakUser user = KeycloakUser.builder()
        .username("test-user")
        .firstName("test")
        .lastName("user")
        .email("test-user@vocabulary.org")
        .isAdmin(false)
        .build();
    //when-then
    assertThatExceptionOfType(DataConflictException.class)
        .isThrownBy(() -> keycloakClientService.createNewUser(user));


  }

}
