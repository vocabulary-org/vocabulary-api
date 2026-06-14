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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.repository.LanguageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class WordLearningRepositoryTest {

  static final int USER_ENRICO_ID = 1000000;
  static final int USER_LUCIO_ID = 1000001;
  static final UUID HELLO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final UUID CAT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000003");
  static final UUID NON_EXISTENT_UUID = UUID.fromString("00000000-0000-0000-0000-999999999999");

  @Autowired
  WordLearningRepository wordLearningRepository;
  @Autowired
  LanguageRepository languageRepository;

  @Test
  void findByWordExternalId_returnsWordLearning() {
    WordLearning result = wordLearningRepository.findByWordExternalId(HELLO_UUID).orElseThrow();
    assertThat(result, notNullValue());
    assertThat(result.wordView().uuid(), equalTo(HELLO_UUID));
    assertThat(result.wordView().sentence(), equalTo("Hello"));
    assertThat(result.wordView().translation(), equalTo("Salve"));
    assertThat(result.rightCount(), equalTo(5));
    assertThat(result.wrongCount(), equalTo(2));
    assertThat(result.skipCount(), equalTo(1));
    assertThat(result.reviewResult(), equalTo(WordReviewResultType.RIGHT));
  }

  @Test
  void findByWordExternalId_returnsEmptyWhenNoLearningRecord() {
    Optional<WordLearning> result = wordLearningRepository.findByWordExternalId(CAT_UUID);
    assertTrue(result.isEmpty());
  }

  @Test
  void findByWordExternalId_throwsDataNotFoundExceptionForNonExistentWord() {
    assertThrows(DataNotFoundException.class,
        () -> wordLearningRepository.findByWordExternalId(NON_EXISTENT_UUID));
  }

  @Test
  void review_incrementsRightCount() {
    WordReviewResult input = new WordReviewResult(HELLO_UUID, WordReviewResultType.RIGHT);
    WordLearning result = wordLearningRepository.review(input);
    assertThat(result.rightCount(), equalTo(6));
    assertThat(result.wrongCount(), equalTo(2));
    assertThat(result.skipCount(), equalTo(1));
    assertThat(result.reviewResult(), equalTo(WordReviewResultType.RIGHT));
  }

  @Test
  void review_incrementsWrongCount() {
    WordReviewResult input = new WordReviewResult(HELLO_UUID, WordReviewResultType.WRONG);
    WordLearning result = wordLearningRepository.review(input);
    assertThat(result.rightCount(), equalTo(5));
    assertThat(result.wrongCount(), equalTo(3));
    assertThat(result.skipCount(), equalTo(1));
    assertThat(result.reviewResult(), equalTo(WordReviewResultType.WRONG));
  }

  @Test
  void review_incrementsSkipCount() {
    WordReviewResult input = new WordReviewResult(HELLO_UUID, WordReviewResultType.SKIP);
    WordLearning result = wordLearningRepository.review(input);
    assertThat(result.rightCount(), equalTo(5));
    assertThat(result.wrongCount(), equalTo(2));
    assertThat(result.skipCount(), equalTo(2));
    assertThat(result.reviewResult(), equalTo(WordReviewResultType.SKIP));
  }

  @Test
  void review_createsNewRecordWhenNoLearningRecordExists() {
    WordReviewResult input = new WordReviewResult(CAT_UUID, WordReviewResultType.RIGHT);
    WordLearning result = wordLearningRepository.review(input);
    assertThat(result, notNullValue());
    assertThat(result.wordView().sentence(), equalTo("cat"));
    assertThat(result.rightCount(), equalTo(1));
    assertThat(result.wrongCount(), equalTo(0));
    assertThat(result.skipCount(), equalTo(0));
    assertThat(result.reviewResult(), equalTo(WordReviewResultType.RIGHT));
  }

  @Test
  void findWordsForReview_returnsUnreviewedFirstThenOrdersByTotalCountAndRightCountAsc() {
    UUID enUuid = languageRepository.findByName("English").orElseThrow().uuid();
    UUID itUuid = languageRepository.findByName("Italian").orElseThrow().uuid();
    List<WordView> result = wordLearningRepository.findWordsForReview(enUuid, itUuid, USER_ENRICO_ID, 10);
    assertThat(result, hasSize(4));
    // 1) unreviewed words come first (cat, tomcat — no WORD_LEARNING record), in any order
    assertThat(result.subList(0, 2).stream().map(WordView::sentence).toList(),
        containsInAnyOrder("cat", "tomcat"));
    // 2) my house: total_count=3, right=0 → lowest total, lowest right → comes first
    assertThat(result.get(2).sentence(), equalTo("my house"));
    // 3) Hello: total_count=8, right=5 → higher total, higher right → comes last
    assertThat(result.get(3).sentence(), equalTo("Hello"));
  }

  @Test
  void findWordsForReview_respectsLimit() {
    UUID enUuid = languageRepository.findByName("English").orElseThrow().uuid();
    UUID itUuid = languageRepository.findByName("Italian").orElseThrow().uuid();
    List<WordView> result = wordLearningRepository.findWordsForReview(enUuid, itUuid, USER_ENRICO_ID, 2);
    assertThat(result, hasSize(2));
    assertThat(result.stream().map(WordView::sentence).toList(),
        containsInAnyOrder("cat", "tomcat"));
  }

  @Test
  void findWordsForReview_filtersbyLanguage() {
    UUID enUuid = languageRepository.findByName("English").orElseThrow().uuid();
    UUID deUuid = languageRepository.findByName("German").orElseThrow().uuid();
    List<WordView> result = wordLearningRepository.findWordsForReview(enUuid, deUuid, USER_ENRICO_ID, 10);
    assertThat(result, hasSize(1));
    assertThat(result.getFirst().sentence(), equalTo("Latte"));
  }

  @Test
  void findWordsForReview_excludesOtherUser() {
    UUID enUuid = languageRepository.findByName("English").orElseThrow().uuid();
    UUID itUuid = languageRepository.findByName("Italian").orElseThrow().uuid();
    List<WordView> result = wordLearningRepository.findWordsForReview(enUuid, itUuid, USER_LUCIO_ID, 10);
    assertThat(result, hasSize(1));
    assertThat(result.getFirst().sentence(), equalTo("Hello"));
  }
}
