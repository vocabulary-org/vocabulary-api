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


import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.LANGUAGE;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.model.Language;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class LanguageRepository {

  public static final String UUID_ALIAS = "uuid";
  public static final String CODE_ALIAS = "code";
  public static final String NAME_ALIAS = "name";
  public static final String ACTIVE_ALIAS = "active";

  private final DSLContext dsl;


  public Optional<Language> findById(Integer id) {
    return getSelect()
        .where(LANGUAGE.ID.eq(id))
        .fetchOptional()
        .map(this::map);
  }

  public Optional<Language> findByName(String name) {
    return getSelect()
        .where(LANGUAGE.NAME.eq(name))
        .fetchOptional()
        .map(this::map);
  }

  public List<Language> findAll() {
    return getSelect()
        .orderBy(LANGUAGE.NAME.asc())
        .fetch()
        .map(this::map);
  }



  private SelectJoinStep<Record4<UUID, String, String, Boolean>> getSelect() {
    return dsl.select(
            LANGUAGE.EXTERNAL_ID.as(UUID_ALIAS),
            LANGUAGE.CODE.as(CODE_ALIAS),
            LANGUAGE.NAME.as(NAME_ALIAS),
            LANGUAGE.ACTIVE.as(ACTIVE_ALIAS))
        .from(LANGUAGE);
  }

  private Language map(Record record) {
    return new Language(
        record.get(UUID_ALIAS, UUID.class),
        record.get(CODE_ALIAS, String.class),
        record.get(NAME_ALIAS, String.class),
        record.get(ACTIVE_ALIAS, Boolean.class)
    );
  }

}
