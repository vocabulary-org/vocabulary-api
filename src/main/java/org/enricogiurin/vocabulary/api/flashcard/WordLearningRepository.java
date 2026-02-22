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

import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.WORD;
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.WORD_LEARNING;

import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.jooq.CustomJooqUtils;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.ReviewResult;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record7;
import org.jooq.SelectOnConditionStep;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WordLearningRepository {

  public static final String UUID_ALIAS = "uuid";
  public static final String SENTENCE_ALIAS = "sentence";
  public static final String TRANSLATION_ALIAS = "translation";
  public static final String RIGHT_COUNT_ALIAS = "rightCount";
  public static final String WRONG_COUNT_ALIAS = "wrongCount";
  public static final String SKIP_COUNT_ALIAS = "skipCount";
  public static final String REVIEW_RESULT_ALIAS = "reviewResult";

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

  private SelectOnConditionStep<Record7<UUID, String, String, Integer, Integer, Integer, ReviewResult>> select() {
    return dsl.select(
            WORD_LEARNING.EXTERNAL_ID.as(UUID_ALIAS),
            WORD.SENTENCE.as(SENTENCE_ALIAS),
            WORD.TRANSLATION.as(TRANSLATION_ALIAS),
            WORD_LEARNING.RIGHT_COUNT.as(RIGHT_COUNT_ALIAS),
            WORD_LEARNING.WRONG_COUNT.as(WRONG_COUNT_ALIAS),
            WORD_LEARNING.SKIP_COUNT.as(SKIP_COUNT_ALIAS),
            WORD_LEARNING.LAST_RESULT.as(REVIEW_RESULT_ALIAS))
        .from(WORD_LEARNING)
        .join(WORD).on(WORD.ID.eq(WORD_LEARNING.WORD_ID));
  }

  private Field<?> getSupportedField(String field) {
    return switch (field) {
      case UUID_ALIAS -> WORD_LEARNING.EXTERNAL_ID;
      case SENTENCE_ALIAS -> WORD.SENTENCE;
      case REVIEW_RESULT_ALIAS -> WORD_LEARNING.LAST_RESULT;
      default -> throw new IllegalArgumentException(
          "Unexpected value for filter/sort field: " + field);
    };
  }

  private WordLearning map(Record record) {
    ReviewResult lastResult = record.get(REVIEW_RESULT_ALIAS, ReviewResult.class);
    WordReviewResult reviewResult = lastResult != null
        ? WordReviewResult.valueOf(lastResult.getLiteral())
        : null;
    return WordLearning.builder()
        .uuid(record.get(UUID_ALIAS, UUID.class))
        .wordView(new WordView(
            record.get(SENTENCE_ALIAS, String.class),
            record.get(TRANSLATION_ALIAS, String.class)))
        .rightCount(record.get(RIGHT_COUNT_ALIAS, Integer.class))
        .wrongCount(record.get(WRONG_COUNT_ALIAS, Integer.class))
        .skipCount(record.get(SKIP_COUNT_ALIAS, Integer.class))
        .reviewResult(reviewResult)
        .build();
  }
}
