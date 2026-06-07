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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.anthropic.AnthropicStoryGenerator;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GapCategory;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GrammaticalCase;
import org.enricogiurin.vocabulary.api.learndeutsch.GeneratedStory;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryGapOptionView;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryGapView;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryLength;
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
class StoryDeAdminControllerTest {

  @Autowired
  MockMvc mvc;

  @Value("${application.api.admin-path}/deutsch/stories")
  String basePath;

  @MockitoBean
  AnthropicStoryGenerator generator;

  private static GeneratedStory validStory() {
    return new GeneratedStory("Marias Tag", "Maria geht in {{1}} Supermarkt.",
        List.of(new StoryGapView(1, GapCategory.ARTICLE, GrammaticalCase.AKKUSATIV, List.of(
            new StoryGapOptionView("der", false),
            new StoryGapOptionView("den", true),
            new StoryGapOptionView("dem", false),
            new StoryGapOptionView("des", false)))));
  }

  @Test
  void generate_validRequest_persistsAndReturnsCreatedStory() throws Exception {
    when(generator.generate(DeutschLevel.A2, "food", StoryLength.SHORT)).thenReturn(validStory());

    mvc.perform(post(basePath + "/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"level":"A2","topic":"food","length":"SHORT"}"""))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.uuid").exists())
        .andExpect(jsonPath("$.title", is("Marias Tag")))
        .andExpect(jsonPath("$.level", is("A2")))
        .andExpect(jsonPath("$.topic", is("food")))
        .andExpect(jsonPath("$.gaps", hasSize(1)))
        .andExpect(jsonPath("$.gaps[0].category", is("ARTICLE")))
        .andExpect(jsonPath("$.gaps[0].options", hasSize(4)))
        .andExpect(jsonPath("$.gaps[0].options[1].text", is("den")))
        .andExpect(jsonPath("$.gaps[0].options[1].correct", is(true)));
  }

  @Test
  void generate_blankTopic_returnsBadRequest() throws Exception {
    mvc.perform(post(basePath + "/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"level":"A2","topic":"","length":"SHORT"}"""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void generate_invalidLevel_returnsBadRequest() throws Exception {
    mvc.perform(post(basePath + "/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"level":"Z9","topic":"food","length":"SHORT"}"""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void generate_generatorReturnsInvalidStory_returnsServerError() throws Exception {
    GeneratedStory mismatch = new GeneratedStory("Bad", "Has {{1}} and {{2}}.",
        validStory().gaps());
    when(generator.generate(any(), any(), any())).thenReturn(mismatch);

    mvc.perform(post(basePath + "/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"level":"B1","topic":"music","length":"MEDIUM"}"""))
        .andExpect(status().is5xxServerError());
  }
}
