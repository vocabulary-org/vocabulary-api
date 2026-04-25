package org.enricogiurin.vocabulary.api.repository;

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
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.User;
import org.enricogiurin.vocabulary.api.repository.UserRepository.InsertResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class UserRepositoryTest {

  private static final String KEYCLOAK_ID = "f95cb50f-5f3b-4b71-9f8b-3495d47622cf";
  private static final Integer USER_ID = 1000000;


  @Autowired
  UserRepository userRepository;



  @Test
  void findById() {
    //when
    User user = userRepository.findById(USER_ID).orElseThrow();
    //then
    assertThat(user).isNotNull();
    assertThat(user.username()).isEqualTo("enrico");
    assertThat(user.email()).isEqualTo("enrico@gmail.com");
    assertThat(user.uuid()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000007"));
  }



  @Test
  void findUserIdByKeycloakId() {
    Integer userId = userRepository.findUserIdByKeycloakId(
        "f95cb50f-5f3b-4b71-9f8b-3495d47622cf").orElseThrow();
    assertThat(userId).isNotNull();
    assertThat(userId).isEqualTo(USER_ID);
  }

  @Test
  void delete() {
    userRepository.findById(USER_ID).orElseThrow();
    boolean isDeleted = userRepository.delete(USER_ID);
    assertThat(isDeleted).isTrue();
  }

  @Test
  void findOrInsert_existingUser() {
    InsertResult result = userRepository.findOrInsert(KEYCLOAK_ID, "enrico");
    assertThat(result.userId()).isEqualTo(USER_ID);
    assertThat(result.created()).isFalse();
  }

  @Test
  void findOrInsert_newUser() {
    String newKeycloakId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    InsertResult result = userRepository.findOrInsert(newKeycloakId, "newuser");
    assertThat(result.userId()).isNotNull();
    assertThat(result.userId()).isNotEqualTo(USER_ID);
    assertThat(result.created()).isTrue();
    assertThat(userRepository.findUserIdByKeycloakId(newKeycloakId)).contains(result.userId());
  }

  @Test
  void findOrInsert_noExceptionOnConcurrentInsertSameKeycloakId() {
    String newKeycloakId = UUID.randomUUID().toString();
    int threadCount = 3;
    CyclicBarrier barrier = new CyclicBarrier(threadCount);
    try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
      List<Future<InsertResult>> futures = new ArrayList<>();
      for (int i = 0; i < threadCount; i++) {
        futures.add(executor.submit(() -> {
          barrier.await();
          return userRepository.findOrInsert(newKeycloakId, "concurrent-user");
        }));
      }
      for (Future<InsertResult> future : futures) {
        assertThatNoException().isThrownBy(() -> future.get(5, TimeUnit.SECONDS));
      }
    }
  }
}
