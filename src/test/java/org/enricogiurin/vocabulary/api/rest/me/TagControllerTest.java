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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.anthropic.AnthropicTagSuggester;
import org.enricogiurin.vocabulary.api.model.TagSuggestion;
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
class TagControllerTest {

  @Autowired
  MockMvc mvc;

  @Value("${application.api.user-path}/tags")
  String basePath;

  @MockitoBean
  AnthropicTagSuggester anthropicTagSuggester;

  @Test
  void suggest_returnsTagSuggestions() throws Exception {
    // given
    String sentence = "domenica vado a venezia in aereo";
    when(anthropicTagSuggester.suggestTags(eq(sentence), eq("it"), anyList()))
        .thenReturn(List.of(
            new TagSuggestion("TRAVEL", "Viaggio"),
            new TagSuggestion("TRANSPORT", "Trasporto"),
            new TagSuggestion("HOLIDAY", "Vacanza")
        ));

    // when / then
    mvc.perform(post(basePath + "/suggest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"sentence": "domenica vado a venezia in aereo", "languageCode": "it"}
                """))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].tag", is("TRAVEL")))
        .andExpect(jsonPath("$[0].label", is("Viaggio")))
        .andExpect(jsonPath("$[1].tag", is("TRANSPORT")))
        .andExpect(jsonPath("$[1].label", is("Trasporto")))
        .andExpect(jsonPath("$[2].tag", is("HOLIDAY")))
        .andExpect(jsonPath("$[2].label", is("Vacanza")));
  }

  @Test
  void suggest_blankSentence_returnsBadRequest() throws Exception {
    mvc.perform(post(basePath + "/suggest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"sentence": ""}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void suggest_missingSentence_returnsBadRequest() throws Exception {
    mvc.perform(post(basePath + "/suggest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }

  private static java.util.List<org.enricogiurin.vocabulary.api.model.Tag> anyList() {
    return org.mockito.ArgumentMatchers.anyList();
  }
}
