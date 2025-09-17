package org.enricogiurin.vocabulary.api.rest.pub;

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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.exception.DataConflictException;
import org.enricogiurin.vocabulary.api.model.KeycloakUser;
import org.enricogiurin.vocabulary.api.service.KeycloakClientService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class RegisterUserControllerTest {

  @Autowired
  MockMvc mvc;

  @Value("${application.api.public-path}/users")
  String basePath;

  @MockitoBean
  KeycloakClientService keycloakClientService;


  @Test
  void createNewKeycloakUser() throws Exception {
    final String username = "mario-rossi";
    mvc.perform(post(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                   "username": "%s",
                   "firstName": "Mario",
                   "lastName": "Rossi",
                   "email": "new-user@vocabolary.org",
                   "isAdmin": "false"
                }
                """.formatted(username)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username", is(username)))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    ArgumentCaptor<KeycloakUser> captor = ArgumentCaptor.forClass(KeycloakUser.class);
    verify(keycloakClientService).createNewUser(captor.capture());
    KeycloakUser captured = captor.getValue();
    assertThat(captured.username()).isEqualTo(username);
    assertThat(captured.isAdmin()).isFalse();
    verify(keycloakClientService).createNewUser(any());

  }

  @Test
  void createNewUser_conflict() throws Exception {
    final String username = "mario-rossi";

    doThrow(DataConflictException.class).when(keycloakClientService).createNewUser(any());
    mvc.perform(post(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                   "username": "%s",
                   "firstName": "Mario",
                   "lastName": "Rossi",
                   "email": "new-user@vocabolary.org",
                   "isAdmin": "false"
                }
                """.formatted(username)))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    verify(keycloakClientService).createNewUser(any());
  }

  @Test
  void createNewUser_badRequest() throws Exception {
    final String username = "mario-rossi";
    mvc.perform(post(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "firstName": "aa",
                   "username": "%s",
                   "lastName": "Rossi",
                   "email": "not-a-valid-email",
                   "isAdmin": "false"
                }
                """.formatted(username)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString(KeycloakUser.EMAIL_CONSTRAINT)))
        .andExpect(jsonPath("$.message", containsString(KeycloakUser.FIRST_NAME_CONSTRAINT)))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    verify(keycloakClientService, never()).createNewUser(any());
  }
}
