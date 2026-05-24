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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.anthropic.AnthropicExampleGenerator;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.Article;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
class NounDeExampleServiceTest {

  @Autowired
  NounDeExampleService service;

  @MockitoBean
  AnthropicExampleGenerator generator;

  @MockitoBean
  PublicNounDeRepository repository;

  @Test
  void generateMissingExamples_allSucceed_returnsCorrectSummary() {
    List<NounForGeneration> nouns = List.of(
        new NounForGeneration(1, Article.der, "Hund", "cane"),
        new NounForGeneration(2, Article.das, "Haus", "casa")
    );
    when(repository.findWithMissingExamples("it", null)).thenReturn(nouns);
    when(generator.generate(eq("der"), eq("Hund"), eq("it"), eq("cane"))).thenReturn(List.of(
        new ExampleView("Mein Hund schläft.", "Il mio cane dorme."),
        new ExampleView("Der Hund ist groß.", "Il cane è grande.")
    ));
    when(generator.generate(eq("das"), eq("Haus"), eq("it"), eq("casa"))).thenReturn(List.of(
        new ExampleView("Das Haus ist neu.", "La casa è nuova.")
    ));

    GenerationSummary result = service.generateMissingExamples("it", null);

    assertThat(result.generated()).isEqualTo(2);
    assertThat(result.failed()).isEqualTo(0);
    verify(repository, times(3)).saveExample(anyInt(), anyString(), anyString());
  }

  @Test
  void generateMissingExamples_oneFails_countsFailure() {
    List<NounForGeneration> nouns = List.of(
        new NounForGeneration(1, Article.der, "Hund", "cane"),
        new NounForGeneration(2, Article.das, "Haus", "casa")
    );
    when(repository.findWithMissingExamples("it", null)).thenReturn(nouns);
    when(generator.generate(eq("der"), eq("Hund"), eq("it"), eq("cane")))
        .thenThrow(new RuntimeException("Claude API error"));
    when(generator.generate(eq("das"), eq("Haus"), eq("it"), eq("casa"))).thenReturn(List.of(
        new ExampleView("Das Haus ist neu.", "La casa è nuova.")
    ));

    GenerationSummary result = service.generateMissingExamples("it", null);

    assertThat(result.generated()).isEqualTo(1);
    assertThat(result.failed()).isEqualTo(1);
    verify(repository, times(1)).saveExample(anyInt(), anyString(), anyString());
  }

  @Test
  void generateMissingExamples_withLimit_passesLimitToRepository() {
    when(repository.findWithMissingExamples("it", 10)).thenReturn(List.of());

    service.generateMissingExamples("it", 10);

    verify(repository).findWithMissingExamples("it", 10);
  }

  @Test
  void generateMissingExamples_noNounsToProcess_returnsZeroSummary() {
    when(repository.findWithMissingExamples("es", null)).thenReturn(List.of());

    GenerationSummary result = service.generateMissingExamples("es", null);

    assertThat(result.generated()).isEqualTo(0);
    assertThat(result.failed()).isEqualTo(0);
    verify(generator, never()).generate(any(), any(), any(), any());
    verify(repository, never()).saveExample(anyInt(), anyString(), anyString());
  }
}
