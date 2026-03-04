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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.Language;
import org.enricogiurin.vocabulary.api.repository.LanguageRepository;
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
class FlashcardControllerTest {

  static final int USER_ENRICO_ID = 1000000;
  static final UUID HELLO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final UUID CAT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000003");
  static final UUID NON_EXISTENT_UUID = UUID.fromString("00000000-0000-0000-0000-999999999999");

  @Autowired
  MockMvc mvc;

  @Autowired
  LanguageRepository languageRepository;

  @MockitoBean
  CurrentUser currentUser;

  @Value("${application.api.user-path}/flashcards")
  String basePath;

  @BeforeEach
  void setUp() {
    when(currentUser.getUserId()).thenReturn(USER_ENRICO_ID);
  }

  @Test
  void findWordsForReview_returnsOrderedList() throws Exception {
    Language english = languageRepository.findByName("English").orElseThrow();
    Language italian = languageRepository.findByName("Italian").orElseThrow();
    mvc.perform(get(basePath + "/words")
            .contentType(MediaType.APPLICATION_JSON)
            .param("language", english.uuid().toString())
            .param("languageTo", italian.uuid().toString())
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(4)))
        // last two are reviewed: my house (total=3, right=0) before Hello (total=8, right=5)
        .andExpect(jsonPath("$[2].sentence", is("my house")))
        .andExpect(jsonPath("$[3].sentence", is("Hello")));
  }

  @Test
  void findWordsForReview_respectsLimit() throws Exception {
    Language english = languageRepository.findByName("English").orElseThrow();
    Language italian = languageRepository.findByName("Italian").orElseThrow();
    mvc.perform(get(basePath + "/words")
            .contentType(MediaType.APPLICATION_JSON)
            .param("language", english.uuid().toString())
            .param("languageTo", italian.uuid().toString())
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  void findByWordUuid_returnsWordLearning() throws Exception {
    mvc.perform(get(basePath + "/words/" + HELLO_UUID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.wordView.sentence", is("Hello")))
        .andExpect(jsonPath("$.wordView.translation", is("Salve")))
        .andExpect(jsonPath("$.wordView.uuid", is(HELLO_UUID.toString())))
        .andExpect(jsonPath("$.rightCount", is(5)))
        .andExpect(jsonPath("$.wrongCount", is(2)))
        .andExpect(jsonPath("$.skipCount", is(1)))
        .andExpect(jsonPath("$.reviewResult", is("RIGHT")));
  }

  @Test
  void findByWordUuid_notFound() throws Exception {
    mvc.perform(get(basePath + "/words/" + NON_EXISTENT_UUID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void review_incrementsRightCount() throws Exception {
    mvc.perform(post(basePath + "/review")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "wordUuid": "%s",
                  "wordReviewResultType": "RIGHT"
                }
                """.formatted(HELLO_UUID)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.wordView.sentence", is("Hello")))
        .andExpect(jsonPath("$.rightCount", is(6)))
        .andExpect(jsonPath("$.reviewResult", is("RIGHT")));
  }

  @Test
  void review_createsNewRecordWhenNoLearningRecordExists() throws Exception {
    mvc.perform(post(basePath + "/review")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "wordUuid": "%s",
                  "wordReviewResultType": "WRONG"
                }
                """.formatted(CAT_UUID)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.wordView.sentence", is("cat")))
        .andExpect(jsonPath("$.rightCount", is(0)))
        .andExpect(jsonPath("$.wrongCount", is(1)))
        .andExpect(jsonPath("$.skipCount", is(0)))
        .andExpect(jsonPath("$.reviewResult", is("WRONG")));
  }

  @Test
  void review_notFound() throws Exception {
    mvc.perform(post(basePath + "/review")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "wordUuid": "%s",
                  "wordReviewResultType": "RIGHT"
                }
                """.formatted(NON_EXISTENT_UUID)))
        .andExpect(status().isNotFound());
  }
}
