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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.DataNotFoundException;
import org.enricogiurin.vocabulary.api.model.User;
import org.enricogiurin.vocabulary.api.model.UserLanguages;
import org.enricogiurin.vocabulary.api.repository.UserLanguagesRepository;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserLanguagesRepository userLanguagesRepository;
  private final UserRepository userRepository;

  public UserLanguages userLanguages(Integer userId) {
    return userLanguagesRepository.findByUserId(userId)
        .orElseGet(() -> new UserLanguages(null, null, null));
  }

  public UserLanguages saveUserLanguages(UserLanguages userLanguages, Integer userId) {
    return userLanguagesRepository.createOrUpdate(userLanguages, userId);
  }

  @Transactional
  public Integer findOrCreateUserIdByKeycloakId(String keycloakId,
      UserCreationAttributes userCreationAttributes) {
    return userRepository.findUserIdByKeycloakId(keycloakId)
        .orElseGet(() ->
        {
          userRepository.add(User.builder()
              .keycloakId(keycloakId)
              .username(userCreationAttributes.username())
              .build());
          log.info("created new user having keycloakId: {}", keycloakId);
          return userRepository.findUserIdByKeycloakId(keycloakId)
              .orElseThrow(
                  () -> new DataNotFoundException(
                      "can't find user having keycloakId: " + keycloakId));
        });
  }

  public record UserCreationAttributes(String username) {
  }

}
