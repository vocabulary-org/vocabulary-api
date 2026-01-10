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

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.Language;
import org.enricogiurin.vocabulary.api.repository.LanguageRepository;
import org.enricogiurin.vocabulary.api.security.CurrentUser;
import org.junit.jupiter.api.Test;
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
class UserLanguagesControllerTest {
  static final int USER_WITH_US_ID = 1000000;
  static final int USER_WITHOUT_UL_ID = 1000001;

  @Autowired
  MockMvc mvc;

  @Autowired
  LanguageRepository languageRepository;

  @Value("${application.api.user-path}/user-languages")
  String basePath;

  @MockitoBean
  CurrentUser currentUser;


  @Test
  void getUserLanguages() throws Exception {
    when(currentUser.getUserId()).thenReturn(USER_WITH_US_ID);
    mvc.perform(get(basePath ).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", is("00000000-0000-0000-0000-000000000001")))
        .andExpect(jsonPath("$.language.name", is("Italian")))
        .andExpect(jsonPath("$.languageTo.name", is("English")));
  }

  @Test
  void getUserLanguages_notFound() throws Exception {
    when(currentUser.getUserId()).thenReturn(USER_WITHOUT_UL_ID);
    mvc.perform(get(basePath ).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void storeUserLanguages() throws Exception {
    when(currentUser.getUserId()).thenReturn(USER_WITH_US_ID);
    Language spanish = languageRepository.findByName("Spanish").orElseThrow();
    Language russian = languageRepository.findByName("Russian").orElseThrow();
    final String body =
        """
            {
              "language":   {"uuid": "%s"},
              "languageTo": {"uuid": "%s"}
            }""".formatted(spanish.uuid(), russian.uuid());
    mvc.perform(put(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", is("00000000-0000-0000-0000-000000000001")))
        .andExpect(jsonPath("$.language.name", is("Spanish")))
        .andExpect(jsonPath("$.languageTo.name", is("Russian")));
  }
}
