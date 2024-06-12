package org.enricogiurin.vocabulary.api.repository;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 Vocabulary Team
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


import static org.enricogiurin.vocabulary.api.mapper.WordMapper.UUID_ALIAS;
import static org.enricogiurin.vocabulary.jooq.Tables.LANGUAGE;
import static org.enricogiurin.vocabulary.jooq.tables.Word.WORD;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LanguageRepository {

  private final DSLContext dsl;

  //TODO - temporary implementation - fix it
  public Optional<UUID> findById(Integer id) {
    return getSelect()
        .where(WORD.ID.eq(id))
        .fetchOptional()
        .map(Record1::value1);
  }

  private SelectJoinStep<Record1<UUID>> getSelect() {
    return dsl.select(
            LANGUAGE.EXTERNAL_ID.as(UUID_ALIAS))
        .from(LANGUAGE);
  }


}
