package org.enricogiurin.vocabulary.api.rest.pub;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.Article;
import org.enricogiurin.vocabulary.api.learndeutsch.NounView;
import org.enricogiurin.vocabulary.api.learndeutsch.PublicNounDeItRepository;
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
class NounDeItControllerTest {

  @Autowired
  MockMvc mvc;

  @Value("${application.api.public-path}/nouns/de-it")
  String basePath;

  @MockitoBean
  PublicNounDeItRepository publicNounDeItRepository;

  @Test
  void getRandom_defaultLimit_returnsOk() throws Exception {
    List<NounView> nouns = List.of(
        new NounView(UUID.randomUUID(), "Haus", Article.das, "Häuser", new String[]{"Hauser", "Hausser"}),
        new NounView(UUID.randomUUID(), "Mann", Article.der, "Männer", new String[]{"Mannen", "Manne"})
    );
    when(publicNounDeItRepository.findRandom(10)).thenReturn(nouns);

    mvc.perform(get(basePath))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)));

    verify(publicNounDeItRepository).findRandom(10);
  }

  @Test
  void getRandom_customLimit_passesLimitToRepository() throws Exception {
    int limit = 5;
    when(publicNounDeItRepository.findRandom(limit)).thenReturn(List.of());

    mvc.perform(get(basePath).param("limit", String.valueOf(limit)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

    verify(publicNounDeItRepository).findRandom(limit);
  }
}
