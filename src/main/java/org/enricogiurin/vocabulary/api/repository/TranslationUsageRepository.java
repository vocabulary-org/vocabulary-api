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


import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.TRANSLATION_USAGE;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TranslationUsageRepository {
  private final DSLContext dsl;

  @Transactional(readOnly = false)
  public long incrementMonthlyCounter(YearMonth yearMonth) {
    LocalDate month = yearMonth.atDay(1);
    Long newCnt = Objects.requireNonNull(dsl.insertInto(TRANSLATION_USAGE)
            .set(TRANSLATION_USAGE.MONTH, month)
            .set(TRANSLATION_USAGE.CNT, 1L)
            .onConflict(TRANSLATION_USAGE.MONTH)
            .doUpdate()
            .set(TRANSLATION_USAGE.CNT, TRANSLATION_USAGE.CNT.plus(1))
            .returning(TRANSLATION_USAGE.CNT)
            .fetchOne())
        .getCnt();
    log.info("new count for yearMonth: {} is {}", yearMonth, newCnt);
    return newCnt != null ? newCnt : 0L;
  }

  @Transactional(readOnly = false)
  public void set(YearMonth yearMonth, long value) {
    LocalDate month = yearMonth.atDay(1);
    dsl.insertInto(TRANSLATION_USAGE)
        .set(TRANSLATION_USAGE.MONTH, month)
        .set(TRANSLATION_USAGE.CNT, value)
        .onConflict(TRANSLATION_USAGE.MONTH)
        .doUpdate()
        .set(TRANSLATION_USAGE.CNT, value)
        .returning(TRANSLATION_USAGE.CNT)
        .fetchOne();
  }


  public long getCurrentMonthCount(YearMonth yearMonth) {
    LocalDate month = yearMonth.atDay(1);
    Long cnt = dsl
        .select(TRANSLATION_USAGE.CNT)
        .from(TRANSLATION_USAGE)
        .where(TRANSLATION_USAGE.MONTH.eq(month))
        .fetchOneInto(Long.class);
    return cnt != null ? cnt : 0L;
  }


}




