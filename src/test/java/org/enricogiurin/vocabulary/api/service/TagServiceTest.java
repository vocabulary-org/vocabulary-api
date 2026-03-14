package org.enricogiurin.vocabulary.api.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.anthropic.AnthropicTagSuggester;
import org.enricogiurin.vocabulary.api.model.TagSuggestion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class TagServiceTest {

  @Autowired
  TagService tagService;

  @MockitoBean
  AnthropicTagSuggester anthropicTagSuggester;

  @Test
  void suggestTags_delegatesToSuggester() {
    // given
    String sentence = "domenica vado a venezia in aereo";
    List<TagSuggestion> mockSuggestions = List.of(
        new TagSuggestion("TRAVEL", "Viaggio"),
        new TagSuggestion("TRANSPORT", "Trasporto"),
        new TagSuggestion("HOLIDAY", "Vacanza")
    );
    when(anthropicTagSuggester.suggestTags(eq(sentence), eq("it")))
        .thenReturn(mockSuggestions);

    // when
    List<TagSuggestion> result = tagService.suggestTags(sentence, "it");

    // then
    assertThat(result).hasSize(3);
    assertThat(result).extracting(TagSuggestion::tag)
        .containsExactly("TRAVEL", "TRANSPORT", "HOLIDAY");
    assertThat(result).extracting(TagSuggestion::label)
        .containsExactly("Viaggio", "Trasporto", "Vacanza");
    verify(anthropicTagSuggester).suggestTags(eq(sentence), eq("it"));
  }
}
