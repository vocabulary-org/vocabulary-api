package org.enricogiurin.vocabulary.api.rest.me;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import org.enricogiurin.vocabulary.api.model.Language;
import org.enricogiurin.vocabulary.api.model.Word;
import org.enricogiurin.vocabulary.api.repository.LanguageRepository;
import org.enricogiurin.vocabulary.api.repository.WordRepository;
import org.enricogiurin.vocabulary.api.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
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
class WordControllerCreateUpdateDeleteTest {

  static final int USER_ENRICO_ID = 1000000;
  static final int HELLO_ID = 1000000;

  @Autowired
  MockMvc mvc;

  @Autowired
  WordRepository wordRepository;

  @Autowired
  LanguageRepository languageRepository;

  @MockitoBean
  CurrentUser currentUser;

  @Value("${application.api.user-path}/words")
  String basePath;


  @BeforeEach
  void setUp() {
    when(currentUser.getUserId()).thenReturn(USER_ENRICO_ID);
  }

  @Test
  void createNewWord() throws Exception {
    Language spanish = languageRepository.findByName("Spanish").orElseThrow();
    Language russian = languageRepository.findByName("Russian").orElseThrow();
    final String body =
        """
            {
              "sentence": "Hola",
              "translation": "Привет",
              "description": "Hola in RU",
              "language":   {"uuid": "%s"},
              "languageTo": {"uuid": "%s"}
            }""".formatted(spanish.uuid(), russian.uuid());
    //System.out.println(body);
    mvc.perform(post(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sentence", is("Hola")))
        .andExpect(jsonPath("$.translation", is("Привет")))
        .andExpect(jsonPath("$.language.name", is("Spanish")))
        .andExpect(jsonPath("$.languageTo.name", is("Russian")))
        .andExpect(jsonPath("$.uuid").isNotEmpty())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void createNewWord_badRequest() throws Exception {
    Language spanish = languageRepository.findByName("Spanish").orElseThrow();
    Language russian = languageRepository.findByName("Russian").orElseThrow();
    mvc.perform(post(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                   "translation": "Привет",
                   "description": "",
                   "language": {"uuid": "%s"},
                   "languageTo": {"uuid": "%s"}
                }""".formatted(spanish.uuid(), russian.uuid())))
        //.andDo(print())   // <--
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString(Word.SENTENCE_NOT_NULL_CONSTRAINT)))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void updateAnExistingWord() throws Exception {
    Word word = wordRepository.findById(HELLO_ID, USER_ENRICO_ID).orElseThrow();
    Language spanish = languageRepository.findByName("Spanish").orElseThrow();

    mvc.perform(patch(basePath + "/" + word.uuid())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                   "languageTo": {"uuid": "%s"},
                   "translation": "Hola"
                }
                """.formatted(spanish.uuid())))
        .andExpect(status().isOk())
        //.andDo(print())
        .andExpect(jsonPath("$.sentence", is("Hello")))
        .andExpect(jsonPath("$.translation", is("Hola")))
        .andExpect(jsonPath("$.language.name", is("English")))
        .andExpect(jsonPath("$.languageTo.name", is("Spanish")))
        .andExpect(jsonPath("$.uuid", is(word.uuid().toString())))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void updateAnExistingWord_badRequest() throws Exception {
    Word word = wordRepository.findById(HELLO_ID, USER_ENRICO_ID).orElseThrow();
    Language spanish = languageRepository.findByName("Spanish").orElseThrow();

    mvc.perform(patch(basePath + "/" + word.uuid())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                   "languageTo": {"uuid": "%s"},
                   "translation": ""
                }
                """.formatted(spanish.uuid())))
        //.andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString(Word.TRANSLATION_CONSTRAINT)))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void deleteAnExistingWord() throws Exception {
    Word word = wordRepository.findById(HELLO_ID, USER_ENRICO_ID).orElseThrow();
    mvc.perform(delete(basePath + "/" + word.uuid()).contentType(
            MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
    assertThat(wordRepository.findById(HELLO_ID, USER_ENRICO_ID).isPresent(), is(false));
  }

  @Test
  void deleteANotExistingWord() throws Exception {
    UUID randomUUID = UUID.randomUUID();
    mvc.perform(
            delete(basePath + "/" + randomUUID).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }


}
