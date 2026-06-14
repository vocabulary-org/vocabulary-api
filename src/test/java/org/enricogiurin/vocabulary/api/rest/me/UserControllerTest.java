package org.enricogiurin.vocabulary.api.rest.me;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 - 2026 Vocabulary Team
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.enricogiurin.vocabulary.api.security.CurrentUser;
import org.enricogiurin.vocabulary.api.service.KeycloakClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class UserControllerTest {

  static final String KEYCLOAK_ID = "f95cb50f-5f3b-4b71-9f8b-3495d47622cf";

  @Autowired
  MockMvc mvc;

  @Autowired
  UserRepository userRepository;
  @MockitoBean
  KeycloakClientService keycloakClientService;

  @MockitoBean
  CurrentUser currentUser;

  @Value("${application.api.user-path}/users")
  String basePath;

  @BeforeEach
  void setUp() {
    when(currentUser.getSubject()).thenReturn(KEYCLOAK_ID);
  }

  @Test
  void deleteUser() throws Exception {
    //given
    userRepository.findUserIdByKeycloakId(KEYCLOAK_ID).orElseThrow();
    // when
    mvc.perform(delete(basePath))
        .andExpect(status().isNoContent());
    assertThat(userRepository.findUserIdByKeycloakId(KEYCLOAK_ID)).isEmpty();
    verify(keycloakClientService).deleteUserFromKeycloak(KEYCLOAK_ID);
  }
}
