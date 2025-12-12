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

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import({VocabularyTestConfiguration.class})
@Transactional
class WordServiceTest {

  static final String KEYCLOAK_ID = "f95cb50f-5f3b-4b71-9f8b-3495d47622cf";
  static final String KEYCLOAK_ID_NEW = "aaa";
  @Autowired
  WordService wordService;
  @Autowired
  private UserRepository userRepository;

  @Test
  void findUserIdByKeycloakId() {
    //given
    userRepository.findUserIdByKeycloakId(KEYCLOAK_ID).orElseThrow();
    //when
    Integer userIdByKeycloakId = wordService.findUserIdByKeycloakId(KEYCLOAK_ID);
    //then
    assertThat(userIdByKeycloakId).isEqualTo(1000000);
  }

  @Test
  void findUserIdByKeycloakId_notExisting() {
    //given
    assertThat(userRepository.findUserIdByKeycloakId(KEYCLOAK_ID_NEW))
        .isEmpty();
    //when
    wordService.findUserIdByKeycloakId(KEYCLOAK_ID_NEW);
    //then
    userRepository.findUserIdByKeycloakId(KEYCLOAK_ID_NEW).orElseThrow();

  }
}
