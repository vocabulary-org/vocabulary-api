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

import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.PUBLIC_STORY_DE;
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.PUBLIC_STORY_DE_GAP;
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.PUBLIC_STORY_DE_GAP_OPTION;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicStoryDeRepository {

  private final DSLContext dsl;

  /**
   * Persists a generated story together with its gaps and options, and returns the new
   * story's public external id. Option sort order follows the order in each gap's list.
   */
  @Transactional
  public UUID save(GeneratedStory story, DeutschLevel level, String topic) {
    var storyRecord = dsl.insertInto(PUBLIC_STORY_DE)
        .set(PUBLIC_STORY_DE.TITLE, story.title())
        .set(PUBLIC_STORY_DE.LEVEL, level)
        .set(PUBLIC_STORY_DE.TOPIC, topic)
        .set(PUBLIC_STORY_DE.BODY, story.body())
        .returning(PUBLIC_STORY_DE.ID, PUBLIC_STORY_DE.EXTERNAL_ID)
        .fetchOne();
    int storyId = storyRecord.get(PUBLIC_STORY_DE.ID);

    for (StoryGapView gap : story.gaps()) {
      int gapId = dsl.insertInto(PUBLIC_STORY_DE_GAP)
          .set(PUBLIC_STORY_DE_GAP.STORY_ID, storyId)
          .set(PUBLIC_STORY_DE_GAP.POSITION, gap.position())
          .set(PUBLIC_STORY_DE_GAP.CATEGORY, gap.category())
          .set(PUBLIC_STORY_DE_GAP.GRAMMATICAL_CASE, gap.grammaticalCase())
          .returning(PUBLIC_STORY_DE_GAP.ID)
          .fetchOne()
          .get(PUBLIC_STORY_DE_GAP.ID);

      int sortOrder = 1;
      for (StoryGapOptionView option : gap.options()) {
        dsl.insertInto(PUBLIC_STORY_DE_GAP_OPTION)
            .set(PUBLIC_STORY_DE_GAP_OPTION.GAP_ID, gapId)
            .set(PUBLIC_STORY_DE_GAP_OPTION.TEXT, option.text())
            .set(PUBLIC_STORY_DE_GAP_OPTION.IS_CORRECT, option.correct())
            .set(PUBLIC_STORY_DE_GAP_OPTION.SORT_ORDER, sortOrder++)
            .execute();
      }
    }
    return storyRecord.get(PUBLIC_STORY_DE.EXTERNAL_ID);
  }

  /**
   * Deletes a story by its public external id. Gaps and options are removed via the
   * {@code ON DELETE CASCADE} foreign keys. Returns true if a story was deleted.
   */
  @Transactional
  public boolean delete(UUID externalId) {
    return dsl.deleteFrom(PUBLIC_STORY_DE)
        .where(PUBLIC_STORY_DE.EXTERNAL_ID.eq(externalId))
        .execute() > 0;
  }

  /**
   * Lists all stories as lightweight summaries (no gaps or options), ordered by level
   * and then title.
   */
  public List<StorySummaryView> findAll() {
    return dsl.select(
            PUBLIC_STORY_DE.EXTERNAL_ID,
            PUBLIC_STORY_DE.TITLE,
            PUBLIC_STORY_DE.LEVEL,
            PUBLIC_STORY_DE.TOPIC)
        .from(PUBLIC_STORY_DE)
        .orderBy(PUBLIC_STORY_DE.LEVEL, PUBLIC_STORY_DE.TITLE)
        .fetch(r -> new StorySummaryView(
            r.get(PUBLIC_STORY_DE.EXTERNAL_ID),
            r.get(PUBLIC_STORY_DE.TITLE),
            r.get(PUBLIC_STORY_DE.LEVEL),
            r.get(PUBLIC_STORY_DE.TOPIC)));
  }

  /**
   * Fetches a complete story (with its gaps and, for each gap, its options) by the
   * public external id. Gaps are ordered by their position, options by their sort order.
   */
  public Optional<StoryView> findByExternalId(UUID externalId) {
    var options = DSL.multiset(
        DSL.select(
                PUBLIC_STORY_DE_GAP_OPTION.TEXT,
                PUBLIC_STORY_DE_GAP_OPTION.IS_CORRECT)
            .from(PUBLIC_STORY_DE_GAP_OPTION)
            .where(PUBLIC_STORY_DE_GAP_OPTION.GAP_ID.eq(PUBLIC_STORY_DE_GAP.ID))
            .orderBy(PUBLIC_STORY_DE_GAP_OPTION.SORT_ORDER)
    ).convertFrom(r -> r.map(o -> new StoryGapOptionView(
        o.get(PUBLIC_STORY_DE_GAP_OPTION.TEXT),
        o.get(PUBLIC_STORY_DE_GAP_OPTION.IS_CORRECT))));

    var gaps = DSL.multiset(
        DSL.select(
                PUBLIC_STORY_DE_GAP.POSITION,
                PUBLIC_STORY_DE_GAP.CATEGORY,
                PUBLIC_STORY_DE_GAP.GRAMMATICAL_CASE,
                options)
            .from(PUBLIC_STORY_DE_GAP)
            .where(PUBLIC_STORY_DE_GAP.STORY_ID.eq(PUBLIC_STORY_DE.ID))
            .orderBy(PUBLIC_STORY_DE_GAP.POSITION)
    ).convertFrom(r -> r.map(g -> new StoryGapView(
        g.get(PUBLIC_STORY_DE_GAP.POSITION),
        g.get(PUBLIC_STORY_DE_GAP.CATEGORY),
        g.get(PUBLIC_STORY_DE_GAP.GRAMMATICAL_CASE),
        g.get(options))));

    return dsl.select(
            PUBLIC_STORY_DE.EXTERNAL_ID,
            PUBLIC_STORY_DE.TITLE,
            PUBLIC_STORY_DE.LEVEL,
            PUBLIC_STORY_DE.TOPIC,
            PUBLIC_STORY_DE.BODY,
            gaps)
        .from(PUBLIC_STORY_DE)
        .where(PUBLIC_STORY_DE.EXTERNAL_ID.eq(externalId))
        .fetchOptional(r -> new StoryView(
            r.get(PUBLIC_STORY_DE.EXTERNAL_ID),
            r.get(PUBLIC_STORY_DE.TITLE),
            r.get(PUBLIC_STORY_DE.LEVEL),
            r.get(PUBLIC_STORY_DE.TOPIC),
            r.get(PUBLIC_STORY_DE.BODY),
            r.get(gaps)));
  }
}
