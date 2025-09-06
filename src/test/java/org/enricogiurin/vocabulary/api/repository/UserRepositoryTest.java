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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class UserRepositoryTest {

  @Autowired
  UserRepository userRepository;



  @Test
  void findById() {
    //when
    User user = userRepository.findById(1000000).orElseThrow();
    //then
    assertThat(user).isNotNull();
    assertThat(user.username()).isEqualTo("enrico");
    assertThat(user.email()).isEqualTo("enrico@gmail.com");
    assertThat(user.uuid()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000007"));
  }


  @Test
  void add() {
    //given
    User newUser = new User(null, "john", "john@gmail.com", "aaa", false);
    //when
    User result = userRepository.add(newUser);
    //then
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isNotNull();
    assertThat(result.username()).isEqualTo("john");
    assertThat(result.email()).isEqualTo("john@gmail.com");
  }

  @Test
  void addAnExistingUser() {
    //given
    userRepository.findByEmail("enrico@gmail.com").orElseThrow();
    User user = new User(null, "john", "enrico@gmail.com", "aaa", false);
    //when-then
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> userRepository.add(user))
        .withMessageContaining("is already present in the User table");

  }

  @Test
  void update() {
    User oldUser = userRepository.findById(1000000).orElseThrow();
    User user = new User(null, "John", "a@google.com", "aaa", true);
    User result = userRepository.update(oldUser.uuid(), user);
    assertThat(result).isNotNull();
    assertThat(result.uuid()).isNotNull();
    assertThat(result.username()).isEqualTo("John");
    assertThat(result.email()).isEqualTo("a@google.com");
    assertThat(result.isAdmin()).isTrue();
  }

  @Test
  void findUserIdByKeycloakId() {
    Integer userId = userRepository.findUserIdByKeycloakId(
        "f95cb50f-5f3b-4b71-9f8b-3495d47622cf");
    assertThat(userId).isNotNull();
    assertThat(userId).isEqualTo(1000000);
  }
}
