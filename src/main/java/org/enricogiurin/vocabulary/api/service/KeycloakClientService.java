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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.enricogiurin.vocabulary.api.exception.DataConflictException;
import org.enricogiurin.vocabulary.api.exception.KeycloakException;
import org.enricogiurin.vocabulary.api.model.KeycloakUser;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class KeycloakClientService {

  static final String REALM_VOCABULARY = "vocabulary";
  static final String CLIENT_ID = "vocabulary-rest-api";
  static final String GROUP_USERS = "vocabulary-users";
  static final int LIFESPAN_IN_SECS = 6_000;

  static final String ACTION_VERIFY_EMAIL = "VERIFY_EMAIL";
  static final String ACTION_UPDATE_PASSWORD = "UPDATE_PASSWORD";

  private final Keycloak keycloakClient;
  private final String redirectUri;
  private final List<String> allowedRedirectUris;
  private final boolean skipEmail;


  KeycloakClientService(final Keycloak keycloakClient,
      @Value("${application.spa.url}") final String redirectUri,
      @Value("${application.cors.allowed-origins}") final List<String> allowedRedirectUris,
      @Value("${application.keycloak-client-service.skip-email:false}") final boolean skipEmail) {
    this.keycloakClient = keycloakClient;
    this.redirectUri = redirectUri;
    this.allowedRedirectUris = allowedRedirectUris;
    this.skipEmail = skipEmail;
  }

  public List<UserRepresentation> userList() {
    List<UserRepresentation> list = keycloakClient.realm(REALM_VOCABULARY)
        .users()
        .list();
    list.forEach(ur -> log.info("user: {}", ur.getUsername()));
    return list;
  }

  @Transactional
  public String createNewUser(KeycloakUser user) {
    return createNewUser(user, null);
  }

  @Transactional
  public String createNewUser(KeycloakUser user, @Nullable String origin) {
    UserRepresentation userRepresentation = getUserRepresentation(user);
    UsersResource usersResource = keycloakClient.realm(REALM_VOCABULARY).users();
    final String userId;
    try (Response response = usersResource.create(userRepresentation)) {
      int status = response.getStatus();
      if (status != HttpStatus.CREATED.value()) {
        String errorMessage = response.readEntity(String.class); // body as string
        log.warn("Failed to create a new user - status: {} - message:\n{}", status,
            errorMessage);
        if (status == HttpStatus.CONFLICT.value()) {
          throw new DataConflictException(
              "user already present: " + userRepresentation.getUsername());
        }
        throw new KeycloakException(
            "Error while creating new user: " + userRepresentation.getUsername() + " - status: "
                + status);
      }
      userId = CreatedResponseUtil.getCreatedId(response);
    }
    UserResource userResource = usersResource.get(userId);
    final String password = randomPassword();
    setPasswordRenew(userResource, password);
    setGroup(userResource);
    sendActionsEmail(userResource, origin);
    log.info("user: {} - userId: {} has been successfully created",
        userRepresentation.getUsername(), userId);
    return userId;
  }

  /**
   * Deletes a user from Keycloak.
   *
   * <p>Note: this only removes the identity from Keycloak. Application data stored in the
   * Vocabulary DB must be deleted separately (ideally in the same business operation).</p>
   *
   * @param keycloakUserId the Keycloak user id (UUID as string)
   */
  public void deleteUserFromKeycloak(final String keycloakUserId) {
    try {
      keycloakClient.realm(REALM_VOCABULARY)
          .users()
          .get(keycloakUserId)
          .remove();
      log.info("Keycloak user {} deleted from realm {}", keycloakUserId, REALM_VOCABULARY);
    } catch (Exception ex) {
      log.warn("Failed to delete Keycloak user {} from realm {}",
          keycloakUserId, REALM_VOCABULARY, ex);
      throw new KeycloakException("Error while deleting user in Keycloak: " + keycloakUserId);
    }
  }
  /*
   ************************ Internal methods *******************************************
   */

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
    GroupsResource groups = keycloakClient.realm(REALM_VOCABULARY).groups();
    GroupRepresentation group = groups.groups().stream()
        .filter(g -> GROUP_USERS.equals(g.getName()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Group not found: " + GROUP_USERS));
    userResource.joinGroup(group.getId());
  }

  void sendActionsEmail(UserResource userResource, String origin) {
    if (skipEmail) {
      log.warn("email to the user won't be sent out!");
      return;
    }
    String redirectUri = resolveRedirectUri(origin);
    userResource.executeActionsEmail(
        CLIENT_ID, redirectUri, LIFESPAN_IN_SECS,
        List.of(ACTION_VERIFY_EMAIL, ACTION_UPDATE_PASSWORD));
  }

  String resolveRedirectUri(String origin) {
    if (origin != null) {
      for (String uri : allowedRedirectUris) {
        if (uri.equals(origin)) {
          return uri;
        }
      }
      log.warn("Origin '{}' not in allowed redirect URIs, falling back to default", origin);
    }
    return redirectUri;
  }

  String randomPassword() {
    return RandomStringUtils.randomAlphanumeric(8);
  }

}
