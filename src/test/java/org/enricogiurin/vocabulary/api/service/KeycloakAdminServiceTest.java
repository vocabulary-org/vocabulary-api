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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.enricogiurin.vocabulary.api.rest.admin.KeycloakUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class KeycloakAdminServiceTest {

  @MockitoBean
  Keycloak keycloak;
  @Autowired
  UserRepository userRepository;

  @Autowired
  KeycloakAdminService keycloakAdminService;

  MockedStatic<CreatedResponseUtil> createdResponseUtil;

  @BeforeEach
  void setUp() {
    RealmResource realm = mock(RealmResource.class);
    UsersResource usersResource = mock(UsersResource.class);
    Response response = mock(Response.class);
    UserResource userResource = mock(UserResource.class);

    when(keycloak.realm(KeycloakAdminService.REALM)).thenReturn(realm);
    when(realm.users()).thenReturn(usersResource);
    when(usersResource.create(any())).thenReturn(response);
    when(response.getStatus()).thenReturn(HttpStatus.CREATED.value());
    when(usersResource.get("userId")).thenReturn(userResource);

    createdResponseUtil = mockStatic(CreatedResponseUtil.class);
    createdResponseUtil
        .when(() -> CreatedResponseUtil.getCreatedId(any(Response.class)))
        .thenReturn("userId");

  }

  @AfterEach
  void tearDown() {
    createdResponseUtil.close();
  }

  //TODO - fix it
  @Disabled
  @Test
  void createNewUser() {

    KeycloakUser keycloakUser = KeycloakUser.builder()
        .email("new-user@vocabulary.org")
        .firstName("John")
        .lastName("Doe")
        .username("johndoe")
        .build();
    keycloakAdminService.createNewUser(keycloakUser);
  }
}
