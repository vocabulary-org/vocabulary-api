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

import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.enricogiurin.vocabulary.api.model.User;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.enricogiurin.vocabulary.api.rest.admin.KeycloakUser;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

  static final String REALM = "vocabulary";
  static final String GROUP_USERS = "vocabulary-users";

  private final Keycloak keycloak;
  private final UserRepository userRepository;

  public List<UserRepresentation> userList() {
    return keycloak.realm(REALM)
        .users()
        .list();
  }

  @Transactional
  public String createNewUser(KeycloakUser user) {
    UserRepresentation userRepresentation = getUserRepresentation(
        user);

    UsersResource usersResource = keycloak.realm(REALM).users();
    String userId;
    try (Response response = usersResource.create(userRepresentation)) {
      if (response.getStatus() != HttpStatus.CREATED.value()) {
        throw new RuntimeException(
            "Error while creating new user: " + userRepresentation.getUsername() + " - status: "
                + response.getStatus());
      }
      userId = CreatedResponseUtil.getCreatedId(response);
      log.info("Created new user with userId: {}", userId);
    }
    UserResource userResource = usersResource.get(userId);
    final String password = randomPassword();
    setPasswordRenew(userResource, password);
    setGroup(userResource);
    saveUser(userResource);
    return password;
  }

  UserRepresentation getUserRepresentation(KeycloakUser user) {
    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setUsername(user.username());                 // username in Keycloak
    userRepresentation.setEmail(user.email());          // required email
    userRepresentation.setFirstName(user.firstName());                // optional
    userRepresentation.setLastName(user.lastName());                 // optional
    userRepresentation.setEnabled(true);                     // enable immediately
    userRepresentation.setEmailVerified(false);              // or true if already verified
    return userRepresentation;
  }

  void setPasswordRenew(UserResource userResource, final String password) {
    CredentialRepresentation cred = new CredentialRepresentation();
    cred.setType(CredentialRepresentation.PASSWORD);
    cred.setValue(password);       // your initial password
    cred.setTemporary(true);    // <-- forces change at first login
    userResource.resetPassword(cred);
  }

  void setGroup(UserResource userResource) {
    GroupsResource groups = keycloak.realm(REALM).groups();
    GroupRepresentation group = groups.groups().stream()
        .filter(g -> GROUP_USERS.equals(g.getName()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Group not found: " + GROUP_USERS));
    userResource.joinGroup(group.getId());
  }

  void saveUser(UserResource userResource) {
    UserRepresentation userRepresentation = userResource.toRepresentation();

    User newUser = User.builder()
        .email(userRepresentation.getEmail())
        .username(userRepresentation.getUsername())
        .keycloakId(userRepresentation.getId())
        .isAdmin(false)
        .build();
    User added = userRepository.add(newUser);
    log.info("Inserted user: {}", added);
  }

  String randomPassword() {
    return RandomStringUtils.randomAlphanumeric(8);
  }


}
