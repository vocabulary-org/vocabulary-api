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

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.DataNotFoundException;
import org.enricogiurin.vocabulary.api.model.KeycloakUser;
import org.enricogiurin.vocabulary.api.model.User;
import org.enricogiurin.vocabulary.api.model.UserLanguages;
import org.enricogiurin.vocabulary.api.notification.EmailService;
import org.enricogiurin.vocabulary.api.repository.UserLanguagesRepository;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserLanguagesRepository userLanguagesRepository;
  private final UserRepository userRepository;
  private final KeycloakClientService keycloakClientService;
  private final EmailService emailService;

  public Optional<UserLanguages> userLanguages(Integer userId) {
    return userLanguagesRepository.findByUserId(userId);
  }

  public UserLanguages saveUserLanguages(UserLanguages userLanguages, Integer userId) {
    return userLanguagesRepository.createOrUpdate(userLanguages, userId);
  }

  @Transactional
  public Integer findOrSaveUserIdByKeycloakId(String keycloakId,
      UserCreationAttributes userCreationAttributes) {
    Integer userId = userRepository.findOrInsert(keycloakId, userCreationAttributes.username());
    boolean isFirstLogin = userRepository.updateLastLogin(keycloakId);
    if (isFirstLogin) {
      log.info("First login for keycloakId: {}", keycloakId);
      emailService.notifyAdmin(
          "New user: " + userCreationAttributes.username(),
          "User '%s' logged in for the first time.".formatted(userCreationAttributes.username()));
    }
    return userId;
  }

  @Transactional
  public String createNewUser(KeycloakUser user) {
    return createNewUser(user, null);
  }

  @Transactional
  public String createNewUser(KeycloakUser user, @Nullable String origin) {
    String keycloakId = keycloakClientService.createNewUser(user, origin);
    User newUser = User.builder()
        .email(user.email())
        .username(user.username())
        .keycloakId(keycloakId)
        .build();
    User added = userRepository.add(newUser);
    log.info("Inserted user: {}", added);
    return keycloakId;
  }

  @Transactional
  public void deleteUserByKeycloakId(String keycloakId) {
    keycloakClientService.deleteUserFromKeycloak(keycloakId);
    Integer userId = userRepository.findUserIdByKeycloakId(keycloakId)
        .orElseThrow(
            () -> new DataNotFoundException(
                "can't find user having keycloakId: " + keycloakId));
    userRepository.delete(userId);
  }

  public record UserCreationAttributes(String username) {
  }

}
