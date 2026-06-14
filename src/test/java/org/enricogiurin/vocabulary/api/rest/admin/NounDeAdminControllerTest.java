package org.enricogiurin.vocabulary.api.rest.admin;

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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.learndeutsch.NounDeExampleService;
import org.enricogiurin.vocabulary.api.learndeutsch.NounDeTranslationService;
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
class NounDeAdminControllerTest {

  @Autowired
  MockMvc mvc;

  @Value("${application.api.admin-path}/deutsch/nouns")
  String basePath;

  @MockitoBean
  NounDeExampleService nounDeExampleService;

  @MockitoBean
  NounDeTranslationService nounDeTranslationService;

  @Test
  void generateExamples_defaultLang_returns202() throws Exception {
    doNothing().when(nounDeExampleService).generateMissingExamplesAsync("it", null);

    mvc.perform(post(basePath + "/examples/generate"))
        .andExpect(status().isAccepted());

    verify(nounDeExampleService).generateMissingExamplesAsync("it", null);
  }

  @Test
  void generateExamples_customLang_passesLangToService() throws Exception {
    doNothing().when(nounDeExampleService).generateMissingExamplesAsync("en", null);

    mvc.perform(post(basePath + "/examples/generate").param("lang", "en"))
        .andExpect(status().isAccepted());

    verify(nounDeExampleService).generateMissingExamplesAsync("en", null);
  }

  @Test
  void generateExamples_spanishLang_returns202() throws Exception {
    doNothing().when(nounDeExampleService).generateMissingExamplesAsync("es", null);

    mvc.perform(post(basePath + "/examples/generate").param("lang", "es"))
        .andExpect(status().isAccepted());

    verify(nounDeExampleService).generateMissingExamplesAsync("es", null);
  }

  @Test
  void generateTranslations_returns202() throws Exception {
    doNothing().when(nounDeTranslationService).generateMissingTranslationsAsync("en", null);

    mvc.perform(post(basePath + "/translations/generate").param("lang", "en"))
        .andExpect(status().isAccepted());

    verify(nounDeTranslationService).generateMissingTranslationsAsync("en", null);
  }

  @Test
  void generateTranslations_withLimit_passesLimitToService() throws Exception {
    doNothing().when(nounDeTranslationService).generateMissingTranslationsAsync("es", 20);

    mvc.perform(post(basePath + "/translations/generate").param("lang", "es").param("limit", "20"))
        .andExpect(status().isAccepted());

    verify(nounDeTranslationService).generateMissingTranslationsAsync("es", 20);
  }

  @Test
  void generateExamples_withLimit_passesLimitToService() throws Exception {
    doNothing().when(nounDeExampleService).generateMissingExamplesAsync("it", 50);

    mvc.perform(post(basePath + "/examples/generate").param("limit", "50"))
        .andExpect(status().isAccepted());

    verify(nounDeExampleService).generateMissingExamplesAsync("it", 50);
  }
}
