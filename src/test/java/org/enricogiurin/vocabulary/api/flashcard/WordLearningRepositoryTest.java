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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class WordLearningRepositoryTest {

  static final UUID HELLO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final UUID MY_HOUSE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  static final UUID CAT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000003");
  static final UUID NON_EXISTENT_UUID = UUID.fromString("00000000-0000-0000-0000-999999999999");

  @Autowired
  WordLearningRepository wordLearningRepository;

  @Test
  void findByWordExternalId_returnsWordLearning() {
    WordLearning result = wordLearningRepository.findByWordExternalId(HELLO_UUID).orElseThrow();
    assertThat(result, notNullValue());
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
  void review_throwsDataNotFoundExceptionWhenNoLearningRecord() {
    WordReviewResult input = new WordReviewResult(CAT_UUID, WordReviewResultType.RIGHT);
    assertThrows(DataNotFoundException.class,
        () -> wordLearningRepository.review(input));
  }
}
