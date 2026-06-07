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
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.PUBLIC_STORY_DE;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GapCategory;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GrammaticalCase;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class PublicStoryDeRepositoryTest {

  @Autowired
  PublicStoryDeRepository repository;

  @Autowired
  DSLContext dsl;

  private UUID externalIdOf(String title) {
    return dsl.select(PUBLIC_STORY_DE.EXTERNAL_ID)
        .from(PUBLIC_STORY_DE)
        .where(PUBLIC_STORY_DE.TITLE.eq(title))
        .fetchOne(PUBLIC_STORY_DE.EXTERNAL_ID);
  }

  @Test
  void findAll_returnsSummariesOrderedByLevelWithoutGaps() {
    List<StorySummaryView> result = repository.findAll();

    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(StorySummaryView::title)
        .containsExactly("Marias Einkauf", "Das Konzert");
    assertThat(result)
        .extracting(StorySummaryView::level)
        .containsExactly(DeutschLevel.A2, DeutschLevel.B1);
    assertThat(result).allSatisfy(s -> assertThat(s.uuid()).isNotNull());
  }

  @Test
  void findByExternalId_returnsCompleteStory() {
    UUID externalId = externalIdOf("Marias Einkauf");

    Optional<StoryView> result = repository.findByExternalId(externalId);

    assertThat(result).isPresent();
    StoryView story = result.get();
    assertThat(story.uuid()).isEqualTo(externalId);
    assertThat(story.title()).isEqualTo("Marias Einkauf");
    assertThat(story.level()).isEqualTo(DeutschLevel.A2);
    assertThat(story.topic()).isEqualTo("food");
    assertThat(story.body()).contains("{{1}}", "{{2}}", "{{3}}");
    assertThat(story.gaps()).hasSize(3);
  }

  @Test
  void findByExternalId_gapsAreOrderedByPosition() {
    StoryView story = repository.findByExternalId(externalIdOf("Marias Einkauf")).orElseThrow();

    assertThat(story.gaps())
        .extracting(StoryGapView::position)
        .containsExactly(1, 2, 3);
  }

  @Test
  void findByExternalId_firstGapHasAlikeOptionsWithSingleCorrect() {
    StoryView story = repository.findByExternalId(externalIdOf("Marias Einkauf")).orElseThrow();

    StoryGapView firstGap = story.gaps().getFirst();
    assertThat(firstGap.category()).isEqualTo(GapCategory.ARTICLE);
    assertThat(firstGap.grammaticalCase()).isEqualTo(GrammaticalCase.AKKUSATIV);
    assertThat(firstGap.options())
        .extracting(StoryGapOptionView::text)
        .containsExactly("der", "den", "dem", "des");
    assertThat(firstGap.options().stream().filter(StoryGapOptionView::correct).toList())
        .singleElement()
        .extracting(StoryGapOptionView::text)
        .isEqualTo("den");
  }

  @Test
  void findByExternalId_verbFormGapHasNullCase() {
    StoryView story = repository.findByExternalId(externalIdOf("Das Konzert")).orElseThrow();

    StoryGapView verbGap = story.gaps().stream()
        .filter(g -> g.category() == GapCategory.VERB_FORM)
        .findFirst()
        .orElseThrow();
    assertThat(verbGap.grammaticalCase()).isNull();
    assertThat(verbGap.options())
        .filteredOn(StoryGapOptionView::correct)
        .singleElement()
        .extracting(StoryGapOptionView::text)
        .isEqualTo("gingen");
  }

  @Test
  void findByExternalId_unknownId_returnsEmpty() {
    assertThat(repository.findByExternalId(UUID.randomUUID())).isEmpty();
  }
}
