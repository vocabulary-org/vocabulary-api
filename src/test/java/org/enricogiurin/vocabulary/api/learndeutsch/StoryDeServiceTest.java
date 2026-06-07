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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.anthropic.AnthropicStoryGenerator;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GapCategory;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GrammaticalCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
class StoryDeServiceTest {

  @Autowired
  StoryDeService service;

  @MockitoBean
  AnthropicStoryGenerator generator;

  @MockitoBean
  PublicStoryDeRepository repository;

  private static StoryGapView gap(int position, boolean exactlyOneCorrect) {
    return new StoryGapView(position, GapCategory.ARTICLE, GrammaticalCase.AKKUSATIV, List.of(
        new StoryGapOptionView("der", false),
        new StoryGapOptionView("den", exactlyOneCorrect),
        new StoryGapOptionView("dem", false),
        new StoryGapOptionView("des", false)));
  }

  private static GeneratedStory validStory() {
    return new GeneratedStory("Marias Tag", "Maria geht in {{1}} Supermarkt.",
        List.of(gap(1, true)));
  }

  @Test
  void generateAndSave_validStory_savesAndReturnsReloadedStory() {
    GeneratedStory generated = validStory();
    UUID id = UUID.randomUUID();
    StoryView saved = new StoryView(id, "Marias Tag", DeutschLevel.A2, "food",
        generated.body(), generated.gaps());
    when(generator.generate(DeutschLevel.A2, "food", StoryLength.SHORT)).thenReturn(generated);
    when(repository.save(generated, DeutschLevel.A2, "food")).thenReturn(id);
    when(repository.findByExternalId(id)).thenReturn(Optional.of(saved));

    StoryView result = service.generateAndSave(DeutschLevel.A2, "food", StoryLength.SHORT);

    assertThat(result).isEqualTo(saved);
    verify(repository).save(generated, DeutschLevel.A2, "food");
  }

  @Test
  void generateAndSave_invalidStory_doesNotSave() {
    GeneratedStory mismatch = new GeneratedStory("Bad", "Text with {{1}} and {{2}}.",
        List.of(gap(1, true)));
    when(generator.generate(any(), eq("food"), any())).thenReturn(mismatch);

    assertThatThrownBy(() -> service.generateAndSave(DeutschLevel.A2, "food", StoryLength.SHORT))
        .isInstanceOf(DataExecutionException.class);

    verify(repository, never()).save(any(), any(), any());
  }

  @Test
  void validate_markersMatchGapsAndSingleCorrect_passes() {
    service.validate(validStory());
  }

  @Test
  void validate_markerWithoutGap_throws() {
    GeneratedStory story = new GeneratedStory("t", "Hat {{1}} und {{2}}.", List.of(gap(1, true)));
    assertThatThrownBy(() -> service.validate(story)).isInstanceOf(DataExecutionException.class);
  }

  @Test
  void validate_noCorrectOption_throws() {
    GeneratedStory story = new GeneratedStory("t", "Hat {{1}}.", List.of(gap(1, false)));
    assertThatThrownBy(() -> service.validate(story)).isInstanceOf(DataExecutionException.class);
  }

  @Test
  void validate_twoCorrectOptions_throws() {
    StoryGapView twoCorrect = new StoryGapView(1, GapCategory.ARTICLE, GrammaticalCase.AKKUSATIV,
        List.of(new StoryGapOptionView("den", true), new StoryGapOptionView("dem", true)));
    GeneratedStory story = new GeneratedStory("t", "Hat {{1}}.", List.of(twoCorrect));
    assertThatThrownBy(() -> service.validate(story)).isInstanceOf(DataExecutionException.class);
  }

  @Test
  void validate_noGaps_throws() {
    GeneratedStory story = new GeneratedStory("t", "Kein Lücke hier.", List.of());
    assertThatThrownBy(() -> service.validate(story)).isInstanceOf(DataExecutionException.class);
  }

  @Test
  void generateFromText_usesDetectedLevelAndCustomTopic_whenNotProvided() {
    GeneratedStory generated = new GeneratedStory("Marias Tag",
        "Maria geht in {{1}} Supermarkt.", List.of(gap(1, true)), DeutschLevel.B1);
    UUID id = UUID.randomUUID();
    StoryView saved = new StoryView(id, "Marias Tag", DeutschLevel.B1, "custom",
        generated.body(), generated.gaps());
    when(generator.generateFromText("Ein Text.", null)).thenReturn(generated);
    when(repository.save(any(), eq(DeutschLevel.B1), eq("custom"))).thenReturn(id);
    when(repository.findByExternalId(id)).thenReturn(Optional.of(saved));

    StoryView result = service.generateFromText("Ein Text.", null, null, null);

    assertThat(result).isEqualTo(saved);
    verify(repository).save(any(), eq(DeutschLevel.B1), eq("custom"));
  }

  @Test
  void generateFromText_usesRequestedLevelOverDetected() {
    GeneratedStory generated = new GeneratedStory("T", "Hat {{1}}.",
        List.of(gap(1, true)), DeutschLevel.B1);
    UUID id = UUID.randomUUID();
    when(generator.generateFromText("Ein Text.", DeutschLevel.A2)).thenReturn(generated);
    when(repository.save(any(), eq(DeutschLevel.A2), eq("custom"))).thenReturn(id);
    when(repository.findByExternalId(id)).thenReturn(Optional.of(
        new StoryView(id, "T", DeutschLevel.A2, "custom", generated.body(), generated.gaps())));

    service.generateFromText("Ein Text.", null, null, DeutschLevel.A2);

    verify(repository).save(any(), eq(DeutschLevel.A2), eq("custom"));
  }

  @Test
  void generateFromText_usesProvidedTitleAndTopic() {
    GeneratedStory generated = new GeneratedStory("AutoTitle", "Hat {{1}}.",
        List.of(gap(1, true)), DeutschLevel.B1);
    UUID id = UUID.randomUUID();
    when(generator.generateFromText("Ein Text.", null)).thenReturn(generated);
    when(repository.save(any(), any(), any())).thenReturn(id);
    when(repository.findByExternalId(id)).thenReturn(Optional.of(
        new StoryView(id, "My Title", DeutschLevel.B1, "myTopic", generated.body(), generated.gaps())));

    service.generateFromText("Ein Text.", "My Title", "myTopic", null);

    verify(repository).save(
        argThat(s -> "My Title".equals(s.title())), eq(DeutschLevel.B1), eq("myTopic"));
  }

  @Test
  void delete_delegatesToRepository() {
    UUID id = UUID.randomUUID();
    when(repository.delete(id)).thenReturn(true);

    assertThat(service.delete(id)).isTrue();

    verify(repository).delete(id);
  }

  @Test
  void generateFromText_levelUndeterminable_throwsAndDoesNotSave() {
    GeneratedStory generated = new GeneratedStory("T", "Hat {{1}}.",
        List.of(gap(1, true)), null);
    when(generator.generateFromText("Ein Text.", null)).thenReturn(generated);

    assertThatThrownBy(() -> service.generateFromText("Ein Text.", null, null, null))
        .isInstanceOf(DataExecutionException.class);

    verify(repository, never()).save(any(), any(), any());
  }
}
