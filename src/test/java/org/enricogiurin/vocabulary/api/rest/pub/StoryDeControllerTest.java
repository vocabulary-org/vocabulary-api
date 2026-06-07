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

import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.PUBLIC_STORY_DE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.jooq.DSLContext;
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
class StoryDeControllerTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  DSLContext dsl;

  @Value("${application.api.public-path}/deutsch/stories")
  String basePath;

  private UUID externalIdOf(String title) {
    return dsl.select(PUBLIC_STORY_DE.EXTERNAL_ID)
        .from(PUBLIC_STORY_DE)
        .where(PUBLIC_STORY_DE.TITLE.eq(title))
        .fetchOne(PUBLIC_STORY_DE.EXTERNAL_ID);
  }

  @Test
  void getStories_returnsSummaryListWithoutGaps() throws Exception {
    mvc.perform(get(basePath))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].uuid").exists())
        .andExpect(jsonPath("$[0].title", is("Marias Einkauf")))
        .andExpect(jsonPath("$[0].level", is("A2")))
        .andExpect(jsonPath("$[0].topic", is("food")))
        .andExpect(jsonPath("$[0].gaps").doesNotExist())
        .andExpect(jsonPath("$[0].body").doesNotExist());
  }

  @Test
  void getStory_existingId_returnsStoryWithGapsAndOptions() throws Exception {
    UUID id = externalIdOf("Marias Einkauf");

    mvc.perform(get(basePath + "/{id}", id))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.uuid", is(id.toString())))
        .andExpect(jsonPath("$.title", is("Marias Einkauf")))
        .andExpect(jsonPath("$.level", is("A2")))
        .andExpect(jsonPath("$.topic", is("food")))
        .andExpect(jsonPath("$.body", containsString("{{1}}")))
        .andExpect(jsonPath("$.gaps", hasSize(3)))
        .andExpect(jsonPath("$.gaps[0].position", is(1)))
        .andExpect(jsonPath("$.gaps[0].category", is("ARTICLE")))
        .andExpect(jsonPath("$.gaps[0].grammaticalCase", is("AKKUSATIV")))
        .andExpect(jsonPath("$.gaps[0].options", hasSize(4)))
        .andExpect(jsonPath("$.gaps[0].options[1].text", is("den")))
        .andExpect(jsonPath("$.gaps[0].options[1].correct", is(true)))
        .andExpect(jsonPath("$.gaps[0].options[0].correct", is(false)));
  }

  @Test
  void getStory_unknownId_returnsNotFound() throws Exception {
    mvc.perform(get(basePath + "/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }
}
