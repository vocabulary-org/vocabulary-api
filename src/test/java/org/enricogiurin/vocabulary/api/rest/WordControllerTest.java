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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.repository.LanguageRepository;
import org.enricogiurin.vocabulary.api.repository.WordRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class WordControllerTest {
  static final int NUM_WORDS = 5;

  @Autowired
  MockMvc mvc;
  @Autowired
  LanguageRepository languageRepository;
  @Autowired
  WordRepository wordRepository;
  @Value("${application.api.basepath}/word")
  String basePath;

  @Test
  void findAll() throws Exception {
    mvc.perform(get(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .param("page", "0")
            .param("size", String.valueOf(Integer.MAX_VALUE)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(NUM_WORDS)))
        .andExpect(jsonPath("$.content[0].sentence", is("cat")))
        .andExpect(jsonPath("$.content[4].sentence", is("tomcat")))
        .andExpect(jsonPath("$.page.totalPages", is(1)))
        .andExpect(jsonPath("$.page.totalElements", is(NUM_WORDS)))
        //TODO - fix this
        //.andExpect(jsonPath("$.page.size", is(2147483647)))
        .andExpect(jsonPath("$.page.number", is(0)));
  }


  @Test
  void findByUuid() {
  }

  @Test
  void add() {
  }

  @Test
  void update() {
  }

  @Test
  void delete() {
  }
}
