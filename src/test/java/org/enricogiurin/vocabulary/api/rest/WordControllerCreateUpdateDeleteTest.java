package org.enricogiurin.vocabulary.api.rest;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.component.AuthenticatedUserProvider;
import org.enricogiurin.vocabulary.api.model.Language;
import org.enricogiurin.vocabulary.api.model.view.WordView;
import org.enricogiurin.vocabulary.api.repository.LanguageRepository;
import org.enricogiurin.vocabulary.api.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class WordControllerCreateUpdateDeleteTest {

  static int HELLO_ID = 1000000;

  @Autowired
  MockMvc mvc;
  @Autowired
  LanguageRepository languageRepository;
  @Autowired
  WordRepository wordRepository;
  @Value("${application.api.basepath}/word")
  String basePath;

  @MockBean
  AuthenticatedUserProvider authenticatedUserProvider;

  @BeforeEach
  void setUp() {
    when(authenticatedUserProvider.getAuthenticatedUserEmail())
        .thenReturn("enrico@gmail.com");
  }

  @Test
  void createNewWord() throws Exception {
    UUID es = languageRepository.findById(4).map(Language::uuid).orElseThrow();
    UUID ru = languageRepository.findById(8).map(Language::uuid).orElseThrow();
    mvc.perform(post(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                   "sentence": "Hola",
                   "translation": "Привет",
                   "description": "Hola in RU",
                   "languageUuid": "%s",
                   "languageToUuid": "%s"
                }
                """.formatted(es, ru)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sentence", is("Hola")))
        .andExpect(jsonPath("$.translation", is("Привет")))
        .andExpect(jsonPath("$.language.nativeName", is("Español")))
        .andExpect(jsonPath("$.languageTo.nativeName", is("Русский")))
        .andExpect(jsonPath("$.uuid").isNotEmpty())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void createNewWordWithANotExistingLanguage() throws Exception {
    UUID es = languageRepository.findById(4).map(Language::uuid).orElseThrow();
    UUID randomUUID = UUID.randomUUID();
    mvc.perform(post(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                   "sentence": "Hola",
                   "translation": "random translation",
                   "description": "Hola in RU",
                   "languageUuid": "%s",
                   "languageToUuid": "%s"
                }
                """.formatted(es, randomUUID)))
        .andExpect(status().is4xxClientError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void updateAnExistingWord() throws Exception {
    UUID es = languageRepository.findById(4).map(Language::uuid).orElseThrow();
    WordView word = wordRepository.findById(HELLO_ID).orElseThrow();
    mvc.perform(patch(basePath + "/" + word.uuid())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                   "languageToUuid": "%s",
                   "translation": "Hola"
                }
                """.formatted(es)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sentence", is("Hello")))
        .andExpect(jsonPath("$.translation", is("Hola")))
        .andExpect(jsonPath("$.language.nativeName", is("English")))
        .andExpect(jsonPath("$.languageTo.nativeName", is("Español")))
        .andExpect(jsonPath("$.uuid", is(word.uuid().toString())))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void deleteAnExistingWord() throws Exception {
    WordView word = wordRepository.findById(HELLO_ID).orElseThrow();
    mvc.perform(delete(basePath + "/" + word.uuid()).contentType(
            MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
    assertThat(wordRepository.findById(HELLO_ID).isPresent(), is(false));
  }

  @Test
  void deleteANotExistingWord() throws Exception {
    UUID randomUUID = UUID.randomUUID();
    mvc.perform(
            delete(basePath + "/" + randomUUID).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }


}
