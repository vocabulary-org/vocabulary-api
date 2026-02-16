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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.KeycloakUser;
import org.enricogiurin.vocabulary.api.model.User;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.enricogiurin.vocabulary.api.service.UserService.UserCreationAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import({VocabularyTestConfiguration.class})
@Transactional
class UserServiceTest {

  static final String KEYCLOAK_ID = "f95cb50f-5f3b-4b71-9f8b-3495d47622cf";
  static final String NEW_USER_KEYCLOAK_ID = "00000000-0000-0000-0000-000000000001";

  static final String NEW_USERNAME = "john-doe";
  static final String USER_EMAIL = "john.doe@example.com";


  @Autowired
  UserService userService;
  @Autowired
  UserRepository userRepository;
  @MockitoBean
  KeycloakClientService keycloakClientService;


  @Test
  void findOrCreate() {

    userRepository.findUserIdByKeycloakId(KEYCLOAK_ID).orElseThrow();
    //when
    Integer userIdByKeycloakId = userService.findOrSaveUserIdByKeycloakId(KEYCLOAK_ID,
        new UserCreationAttributes("enrico"));
    //then
    assertThat(userIdByKeycloakId).isEqualTo(1000000);
    verify(keycloakClientService, never()).createNewUser(any(), any());
  }

  @Test
  void findOrCreate_notExisting() {
    //given
    assertThat(userRepository.findUserIdByKeycloakId(NEW_USER_KEYCLOAK_ID))
        .isEmpty();
    //when
    userService.findOrSaveUserIdByKeycloakId(NEW_USER_KEYCLOAK_ID,
        new UserCreationAttributes("enrico"));
    //then
    userRepository.findUserIdByKeycloakId(NEW_USER_KEYCLOAK_ID).orElseThrow();
    verify(keycloakClientService, never()).createNewUser(any(), any());

  }

  @Test
  void createNewUser() {
    //given
    assertThat(userRepository.findUserIdByKeycloakId(NEW_USER_KEYCLOAK_ID))
        .isEmpty();
    KeycloakUser user = KeycloakUser.builder()
        .username(NEW_USERNAME)
        .firstName("John")
        .lastName("Doe")
        .email(USER_EMAIL)
        .isAdmin(false)
        .build();
    when(keycloakClientService.createNewUser(any(), any())).thenReturn(NEW_USER_KEYCLOAK_ID);

    //when
    String keyCloakId = userService.createNewUser(user, null);

    //then
    assertThat(keyCloakId).isEqualTo(NEW_USER_KEYCLOAK_ID);
    verify(keycloakClientService).createNewUser(user, null);
    Integer userId = userRepository.findUserIdByKeycloakId(NEW_USER_KEYCLOAK_ID).orElseThrow();
    User newUser = userRepository.findById(userId).orElseThrow();
    assertThat(newUser)
        .extracting(
            User::username,
            User::email,
            User::keycloakId
        )
        .containsExactly(
            NEW_USERNAME,
            USER_EMAIL,
            NEW_USER_KEYCLOAK_ID
        );
  }

  @Test
  void deleteUserByKeycloakId() {
    //given
    userRepository.findUserIdByKeycloakId(KEYCLOAK_ID).orElseThrow();

    //when
    userService.deleteUserByKeycloakId(KEYCLOAK_ID);

    assertThat(userRepository.findUserIdByKeycloakId(NEW_USER_KEYCLOAK_ID))
        .isEmpty();
    verify(keycloakClientService).deleteUserFromKeycloak(KEYCLOAK_ID);
  }
}
