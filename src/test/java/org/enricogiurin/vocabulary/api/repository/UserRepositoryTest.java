package org.enricogiurin.vocabulary.api.repository;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 Vocabulary Team
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
import static org.mockito.Mockito.when;

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.component.AuthenticatedUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class UserRepositoryTest {

  @Autowired
  UserRepository userRepository;

  @MockBean
  AuthenticatedUserProvider authenticatedUserProvider;

  @BeforeEach
  void setUp() {
    when(authenticatedUserProvider.getAuthenticatedUserEmail())
        .thenReturn("enrico@gmail.com");
  }

  @Test
  void findUserIdByAuthenticatedEmail() {
    Integer userIdByAuthenticatedEmail = userRepository.findUserIdByAuthenticatedEmail();
    assertThat(userIdByAuthenticatedEmail).isEqualTo(1000000);
  }
}
