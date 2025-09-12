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
import static org.enricogiurin.vocabulary.api.service.KeycloakClientService.REALM_VOCABULARY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.enricogiurin.vocabulary.api.exception.KeyCloakException;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.enricogiurin.vocabulary.api.rest.pub.KeycloakUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class KeycloakClientServiceTest {

  private static final String USER_EMAIL = "john.doe@example.com";

  @MockitoBean
  UserRepository userRepository;
  @MockitoBean
  Keycloak keycloakAdminClient;

  KeycloakClientService keycloakClientService;


  @BeforeEach
  void setUp() {
    this.keycloakClientService = new KeycloakClientService(keycloakAdminClient, userRepository, "",
        true);
  }

  @Test
  void createNewUser_conflict() {
    //given
    KeycloakUser user = KeycloakUser.builder()
        .username("johndoe")
        .firstName("John")
        .lastName("Doe")
        .email(USER_EMAIL)
        .isAdmin(false)
        .build();
    UsersResource usersResource = mock(UsersResource.class);
    Response response = mock(Response.class);
    RealmResource realmResource = mock(RealmResource.class);
    when(response.getStatus())
        .thenReturn(HttpStatus.CONFLICT.value())
        .thenReturn(HttpStatus.CONFLICT.value());  //it's called twice
    when(response.readEntity(String.class))
        .thenReturn("conflict");
    when(keycloakAdminClient.realm(REALM_VOCABULARY)).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);
    ArgumentCaptor<UserRepresentation> acUserRepresentation = ArgumentCaptor.forClass(
        UserRepresentation.class);
    when(usersResource.create(acUserRepresentation.capture())).thenReturn(response);

    //when
    assertThatExceptionOfType(KeyCloakException.class)
        .isThrownBy(() -> keycloakClientService.createNewUser(user))
        .withMessageContaining("" + HttpStatus.CONFLICT.value());
    //then
    UserRepresentation userRepresentation = acUserRepresentation.getValue();
    assertThat(userRepresentation.getUsername()).isEqualTo(user.username());
    assertThat(userRepresentation.getEmail()).isEqualTo(user.email());
    assertThat(userRepresentation.getFirstName()).isEqualTo(user.firstName());
    assertThat(userRepresentation.getLastName()).isEqualTo(user.lastName());
    verify(userRepository, never()).add(any());
  }

}
