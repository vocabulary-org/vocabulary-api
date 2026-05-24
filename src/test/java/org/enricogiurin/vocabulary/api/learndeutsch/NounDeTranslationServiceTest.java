package org.enricogiurin.vocabulary.api.learndeutsch;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.anthropic.AnthropicTranslationGenerator;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.Article;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
class NounDeTranslationServiceTest {

  @Autowired
  NounDeTranslationService service;

  @MockitoBean
  AnthropicTranslationGenerator generator;

  @MockitoBean
  PublicNounDeRepository repository;

  @Test
  void generateMissingTranslations_allSucceed_returnsCorrectSummary() {
    List<NounForTranslation> nouns = List.of(
        new NounForTranslation(1, Article.der, "Hund"),
        new NounForTranslation(2, Article.die, "Birne")
    );
    when(repository.findWithMissingTranslation("en", null)).thenReturn(nouns);
    when(generator.generate("der", "Hund", "en")).thenReturn("dog");
    when(generator.generate("die", "Birne", "en")).thenReturn("pear");

    GenerationSummary result = service.generateMissingTranslations("en", null);

    assertThat(result.generated()).isEqualTo(2);
    assertThat(result.failed()).isEqualTo(0);
    verify(repository).saveTranslation(1, "en", "dog");
    verify(repository).saveTranslation(2, "en", "pear");
  }

  @Test
  void generateMissingTranslations_oneFails_countsFailure() {
    List<NounForTranslation> nouns = List.of(
        new NounForTranslation(1, Article.der, "Hund"),
        new NounForTranslation(2, Article.die, "Birne")
    );
    when(repository.findWithMissingTranslation("es", null)).thenReturn(nouns);
    when(generator.generate("der", "Hund", "es")).thenThrow(new RuntimeException("Claude API error"));
    when(generator.generate("die", "Birne", "es")).thenReturn("pera");

    GenerationSummary result = service.generateMissingTranslations("es", null);

    assertThat(result.generated()).isEqualTo(1);
    assertThat(result.failed()).isEqualTo(1);
    verify(repository, times(1)).saveTranslation(anyInt(), anyString(), anyString());
  }

  @Test
  void generateMissingTranslations_withLimit_passesLimitToRepository() {
    when(repository.findWithMissingTranslation("en", 10)).thenReturn(List.of());

    service.generateMissingTranslations("en", 10);

    verify(repository).findWithMissingTranslation("en", 10);
  }

  @Test
  void generateMissingTranslations_noNounsToProcess_returnsZeroSummary() {
    when(repository.findWithMissingTranslation(eq("en"), eq(null))).thenReturn(List.of());

    GenerationSummary result = service.generateMissingTranslations("en", null);

    assertThat(result.generated()).isEqualTo(0);
    assertThat(result.failed()).isEqualTo(0);
    verify(generator, never()).generate(anyString(), anyString(), anyString());
    verify(repository, never()).saveTranslation(anyInt(), anyString(), anyString());
  }
}
