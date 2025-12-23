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
  void incrementMonthlyCounter_notExisting() {
    YearMonth august2025 = YearMonth.of(2025, Month.AUGUST);
    long cnt = translationUsageRepository.incrementMonthlyCounter(august2025);
    assertThat(cnt).isEqualTo(1);
  }

  @Test
  void incrementMonthlyCounter() {
    YearMonth august2025 = YearMonth.of(2025, Month.JULY);
    long cnt = translationUsageRepository.incrementMonthlyCounter(august2025);
    assertThat(cnt).isEqualTo(6);
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
