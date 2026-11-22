package org.enricogiurin.vocabulary.api.repository;

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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.time.Month;
import java.time.YearMonth;
import java.util.Optional;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class TranslationUsageRepositoryTest {

  @Autowired
  TranslationUsageRepository translationUsageRepository;

  @Test
  void incrementMonthlyCounterIfWithin_notExisting() {
    YearMonth august2025 = YearMonth.of(2025, Month.AUGUST);
    Optional<Long> cnt = translationUsageRepository.incrementMonthlyCounterIfWithin(august2025, 3, 10);
    assertThat(cnt).contains(3L);
  }

  @Test
  void incrementMonthlyCounterIfWithin() {
    // July 2025 starts at 5 (test data)
    YearMonth july2025 = YearMonth.of(2025, Month.JULY);
    Optional<Long> cnt = translationUsageRepository.incrementMonthlyCounterIfWithin(july2025, 2, 10);
    assertThat(cnt).contains(7L);
  }

  @Test
  void incrementMonthlyCounterIfWithin_upToMax() {
    YearMonth july2025 = YearMonth.of(2025, Month.JULY);
    Optional<Long> cnt = translationUsageRepository.incrementMonthlyCounterIfWithin(july2025, 5, 10);
    assertThat(cnt).contains(10L);
  }

  @Test
  void incrementMonthlyCounterIfWithin_overMax_isRejectedAndNotCounted() {
    YearMonth july2025 = YearMonth.of(2025, Month.JULY);
    Optional<Long> cnt = translationUsageRepository.incrementMonthlyCounterIfWithin(july2025, 6, 10);
    assertThat(cnt).isEmpty();
    assertThat(translationUsageRepository.getCurrentMonthCount(july2025)).isEqualTo(5L);
  }

  @Test
  void incrementMonthlyCounterIfWithin_unitsOverMax_newMonth_isRejected() {
    YearMonth august2025 = YearMonth.of(2025, Month.AUGUST);
    Optional<Long> cnt = translationUsageRepository.incrementMonthlyCounterIfWithin(august2025, 11, 10);
    assertThat(cnt).isEmpty();
    assertThat(translationUsageRepository.getCurrentMonthCount(august2025)).isEqualTo(0L);
  }

  @Test
  void getCurrentMonthCount() {
    YearMonth july2025 = YearMonth.of(2025, Month.JULY);
    long currentMonthCount = translationUsageRepository.getCurrentMonthCount(july2025);
    assertThat(currentMonthCount).isEqualTo(5L);
  }

  @Test
  void getCurrentMonthCount_notExisting() {
    YearMonth august2025 = YearMonth.of(2025, Month.AUGUST);
    long currentMonthCount = translationUsageRepository.getCurrentMonthCount(august2025);
    assertThat(currentMonthCount).isEqualTo(0L);
  }
}
