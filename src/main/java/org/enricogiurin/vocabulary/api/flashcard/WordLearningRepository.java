package org.enricogiurin.vocabulary.api.flashcard;

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

import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.LANGUAGE;
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.WORD;
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.WORD_LEARNING;

import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.jooq.CustomJooqUtils;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.ReviewResult;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.tables.records.WordLearningRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record8;
import org.jooq.SelectOnConditionStep;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WordLearningRepository {

  public static final String UUID_ALIAS = "uuid";
  public static final String WORD_UUID_ALIAS = "wordUuid";
  public static final String SENTENCE_ALIAS = "sentence";
  public static final String TRANSLATION_ALIAS = "translation";
  public static final String RIGHT_COUNT_ALIAS = "rightCount";
  public static final String WRONG_COUNT_ALIAS = "wrongCount";
  public static final String SKIP_COUNT_ALIAS = "skipCount";
  public static final String REVIEW_RESULT_ALIAS = "reviewResult";

  static final int COOLDOWN_HOURS = 24;

  private final DSLContext dsl;
  private final CustomJooqUtils jooqUtils;

  public Optional<WordLearning> findByWordExternalId(UUID wordExternalId) {
    if (!dsl.fetchExists(WORD, WORD.EXTERNAL_ID.eq(wordExternalId))) {
      throw new DataNotFoundException("Word not found: " + wordExternalId);
    }
    return select()
        .where(WORD.EXTERNAL_ID.eq(wordExternalId))
        .fetchOptional()
        .map(this::map);
  }

  @Transactional(readOnly = false)
  public WordLearning review(WordReviewResult wordReviewResult) {
    UUID wordUuid = wordReviewResult.wordUuid();
    Integer wordId = dsl.select(WORD.ID).from(WORD)
        .where(WORD.EXTERNAL_ID.eq(wordUuid))
        .fetchOptional(WORD.ID)
        .orElseThrow(() -> new DataNotFoundException("Word not found: " + wordUuid));
    WordLearningRecord record = dsl.selectFrom(WORD_LEARNING)
        .where(WORD_LEARNING.WORD_ID.eq(wordId))
        .fetchOptional()
        .orElseGet(() -> {
          WordLearningRecord newRecord = dsl.newRecord(WORD_LEARNING);
          newRecord.setWordId(wordId);
          newRecord.setRightCount(0);
          newRecord.setWrongCount(0);
          newRecord.setSkipCount(0);
          newRecord.insert();
          return newRecord;
        });
    switch (wordReviewResult.wordReviewResultType()) {
      case RIGHT -> record.setRightCount(record.getRightCount() + 1);
      case WRONG -> record.setWrongCount(record.getWrongCount() + 1);
      case SKIP -> record.setSkipCount(record.getSkipCount() + 1);
    }
    record.setLastResult(ReviewResult.valueOf(wordReviewResult.wordReviewResultType().name()));
    record.setLastReviewedAt(OffsetDateTime.now());
    record.update();
    return findByWordExternalId(wordUuid).orElseThrow();
  }

  public List<WordView> findWordsForReview(UUID languageUuid, UUID languageToUuid, Integer userId, int limit) {
    var lFrom = LANGUAGE.as("l_from");
    var lTo = LANGUAGE.as("l_to");

    // Step 1: words never reviewed (no word_learning record) — highest priority
    List<WordView> unreviewed = dsl.select(WORD.EXTERNAL_ID, WORD.SENTENCE, WORD.TRANSLATION)
        .from(WORD)
        .leftJoin(WORD_LEARNING).on(WORD_LEARNING.WORD_ID.eq(WORD.ID))
        .join(lFrom).on(lFrom.ID.eq(WORD.LANGUAGE_ID))
        .join(lTo).on(lTo.ID.eq(WORD.LANGUAGE_TO_ID))
        .where(lFrom.EXTERNAL_ID.eq(languageUuid))
        .and(lTo.EXTERNAL_ID.eq(languageToUuid))
        .and(WORD.USER_ID.eq(userId))
        .and(WORD_LEARNING.ID.isNull())
        .fetch(r -> new WordView(r.get(WORD.EXTERNAL_ID), r.get(WORD.SENTENCE), r.get(WORD.TRANSLATION)));

    if (unreviewed.size() >= limit) {
      return unreviewed.subList(0, limit);
    }

    int remaining = limit - unreviewed.size();
    List<UUID> excludeUuids = unreviewed.stream().map(WordView::uuid).toList();
    OffsetDateTime cooldownThreshold = OffsetDateTime.now().minusHours(COOLDOWN_HOURS);
    var totalCount = WORD_LEARNING.RIGHT_COUNT.plus(WORD_LEARNING.WRONG_COUNT).plus(WORD_LEARNING.SKIP_COUNT);

    Condition reviewedBase = lFrom.EXTERNAL_ID.eq(languageUuid)
        .and(lTo.EXTERNAL_ID.eq(languageToUuid))
        .and(WORD.USER_ID.eq(userId));
    if (!excludeUuids.isEmpty()) {
      reviewedBase = reviewedBase.and(WORD.EXTERNAL_ID.notIn(excludeUuids));
    }

    // Step 2: reviewed words outside the cooldown window — ordered by total_count ASC, right_count ASC
    List<WordView> cooledDown = dsl.select(WORD.EXTERNAL_ID, WORD.SENTENCE, WORD.TRANSLATION)
        .from(WORD)
        .join(WORD_LEARNING).on(WORD_LEARNING.WORD_ID.eq(WORD.ID))
        .join(lFrom).on(lFrom.ID.eq(WORD.LANGUAGE_ID))
        .join(lTo).on(lTo.ID.eq(WORD.LANGUAGE_TO_ID))
        .where(reviewedBase)
        .and(WORD_LEARNING.LAST_REVIEWED_AT.le(cooldownThreshold))
        .orderBy(totalCount.asc(), WORD_LEARNING.RIGHT_COUNT.asc())
        .limit(remaining)
        .fetch(r -> new WordView(r.get(WORD.EXTERNAL_ID), r.get(WORD.SENTENCE), r.get(WORD.TRANSLATION)));

    if (cooledDown.size() >= remaining) {
      List<WordView> result = new ArrayList<>(unreviewed);
      result.addAll(cooledDown);
      return result;
    }

    // Step 3: fallback — fill remaining slots with recently reviewed words (least recent first)
    int stillRemaining = remaining - cooledDown.size();
    List<UUID> alreadySelected = new ArrayList<>(excludeUuids);
    cooledDown.stream().map(WordView::uuid).forEach(alreadySelected::add);

    Condition fallbackCondition = lFrom.EXTERNAL_ID.eq(languageUuid)
        .and(lTo.EXTERNAL_ID.eq(languageToUuid))
        .and(WORD.USER_ID.eq(userId))
        .and(WORD_LEARNING.LAST_REVIEWED_AT.gt(cooldownThreshold));
    if (!alreadySelected.isEmpty()) {
      fallbackCondition = fallbackCondition.and(WORD.EXTERNAL_ID.notIn(alreadySelected));
    }

    List<WordView> recentFallback = dsl.select(WORD.EXTERNAL_ID, WORD.SENTENCE, WORD.TRANSLATION)
        .from(WORD)
        .join(WORD_LEARNING).on(WORD_LEARNING.WORD_ID.eq(WORD.ID))
        .join(lFrom).on(lFrom.ID.eq(WORD.LANGUAGE_ID))
        .join(lTo).on(lTo.ID.eq(WORD.LANGUAGE_TO_ID))
        .where(fallbackCondition)
        .orderBy(WORD_LEARNING.LAST_REVIEWED_AT.asc(), totalCount.asc())
        .limit(stillRemaining)
        .fetch(r -> new WordView(r.get(WORD.EXTERNAL_ID), r.get(WORD.SENTENCE), r.get(WORD.TRANSLATION)));

    List<WordView> result = new ArrayList<>(unreviewed);
    result.addAll(cooledDown);
    result.addAll(recentFallback);
    return result;
  }

  private SelectOnConditionStep<Record8<UUID, UUID, String, String, Integer, Integer, Integer, ReviewResult>> select() {
    return dsl.select(
            WORD_LEARNING.EXTERNAL_ID.as(UUID_ALIAS),
            WORD.EXTERNAL_ID.as(WORD_UUID_ALIAS),
            WORD.SENTENCE.as(SENTENCE_ALIAS),
            WORD.TRANSLATION.as(TRANSLATION_ALIAS),
            WORD_LEARNING.RIGHT_COUNT.as(RIGHT_COUNT_ALIAS),
            WORD_LEARNING.WRONG_COUNT.as(WRONG_COUNT_ALIAS),
            WORD_LEARNING.SKIP_COUNT.as(SKIP_COUNT_ALIAS),
            WORD_LEARNING.LAST_RESULT.as(REVIEW_RESULT_ALIAS))
        .from(WORD_LEARNING)
        .join(WORD).on(WORD.ID.eq(WORD_LEARNING.WORD_ID));
  }

  private WordLearning map(Record record) {
    ReviewResult lastResult = record.get(REVIEW_RESULT_ALIAS, ReviewResult.class);
    WordReviewResultType reviewResult = lastResult != null
        ? WordReviewResultType.valueOf(lastResult.getLiteral())
        : null;
    return WordLearning.builder()
        .uuid(record.get(UUID_ALIAS, UUID.class))
        .wordView(new WordView(
            record.get(WORD_UUID_ALIAS, UUID.class),
            record.get(SENTENCE_ALIAS, String.class),
            record.get(TRANSLATION_ALIAS, String.class)))
        .rightCount(record.get(RIGHT_COUNT_ALIAS, Integer.class))
        .wrongCount(record.get(WRONG_COUNT_ALIAS, Integer.class))
        .skipCount(record.get(SKIP_COUNT_ALIAS, Integer.class))
        .reviewResult(reviewResult)
        .build();
  }
}
