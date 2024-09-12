package org.enricogiurin.vocabulary.api.rest.admin;

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

import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.Language;
import org.enricogiurin.vocabulary.api.repository.LanguageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class LanguageControllerTest {

  static final int NUM_LANGUAGES = 20;

  @Autowired
  MockMvc mvc;
  @Autowired
  LanguageRepository languageRepository;

  @Value("${application.api.admin-path}/language")
  String basePath;

  @Test
  void findAllWithPageSize5() throws Exception {
    mvc.perform(get(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .param("page", "0")
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(5)))
        .andExpect(jsonPath("$.content[0].name", is("Arabic")))
        .andExpect(jsonPath("$.content[0].nativeName", is("العربية")))
        .andExpect(jsonPath("$.content[1].name", is("Bengali")))
        .andExpect(jsonPath("$.content[1].nativeName", is("বাংলা")))
        .andExpect(jsonPath("$.page.totalPages", is(4)))
        .andExpect(jsonPath("$.page.totalElements", is(NUM_LANGUAGES)))
        .andExpect(jsonPath("$.page.size", is(5)))
        .andExpect(jsonPath("$.page.number", is(0)));
  }

  @Test
  void findByUuid() throws Exception {
    UUID es = languageRepository.findById(4).map(Language::uuid).orElseThrow();
    mvc.perform(get(basePath + "/" + es).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid").isNotEmpty())
        .andExpect(jsonPath("$.code", is("es")))
        .andExpect(jsonPath("$.name", is("Spanish")))
        .andExpect(jsonPath("$.nativeName", is("Español")));
  }
}
