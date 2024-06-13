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


import static org.enricogiurin.vocabulary.jooq.Tables.LANGUAGE;

import com.yourrents.services.common.searchable.Searchable;
import com.yourrents.services.common.util.exception.DataNotFoundException;
import com.yourrents.services.common.util.jooq.JooqUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.model.Language;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LanguageRepository {

  public static final String UUID_ALIAS = "uuid";
  public static final String NAME_ALIAS = "name";
  public static final String CODE_ALIAS = "code";
  public static final String NATIVE_NAME_ALIAS = "nativeName";

  private final DSLContext dsl;
  private final JooqUtils jooqUtils;

  public Optional<Language> findByExternalId(UUID externalId) {
    return getSelect()
        .where(LANGUAGE.EXTERNAL_ID.eq(externalId))
        .fetchOptional()
        .map(this::map);
  }

  public Optional<Language> findById(Integer id) {
    return getSelect()
        .where(LANGUAGE.ID.eq(id))
        .fetchOptional()
        .map(this::map);
  }

  public Integer findLanguageIdByUuid(UUID languageUuid) {
    return dsl.select(LANGUAGE.ID)
        .from(LANGUAGE)
        .where(LANGUAGE.EXTERNAL_ID.eq(languageUuid))
        .fetchOptional(LANGUAGE.ID).orElseThrow(
            () -> new DataNotFoundException("Language not found: "
                + languageUuid));
  }

  public Page<Language> find(Searchable filter, Pageable pageable) {
    Select<?> result = jooqUtils.paginate(
        dsl,
        jooqUtils.getQueryWithConditionsAndSorts(getSelect(),
            filter, this::getSupportedField,
            pageable, this::getSupportedField),
        pageable.getPageSize(), pageable.getOffset());

    List<Language> words = result.fetch(this::map);
    int totalRows = Objects.requireNonNullElse(
        result.fetchAny("total_rows", Integer.class), 0);
    return new PageImpl<>(words, pageable, totalRows);
  }

  private SelectJoinStep<Record4<UUID, String, String, String>> getSelect() {
    return dsl.select(
            LANGUAGE.EXTERNAL_ID.as(UUID_ALIAS),
            LANGUAGE.NAME.as(NAME_ALIAS),
            LANGUAGE.CODE.as(CODE_ALIAS),
            LANGUAGE.NATIVE_NAME.as(NATIVE_NAME_ALIAS)
        )
        .from(LANGUAGE);
  }

  private Field<?> getSupportedField(String field) {
    return switch (field) {
      case UUID_ALIAS -> LANGUAGE.EXTERNAL_ID;
      case CODE_ALIAS -> LANGUAGE.CODE;
      case NAME_ALIAS -> LANGUAGE.NAME;
      default -> throw new IllegalArgumentException(
          "Unexpected value for filter/sort field: " + field);
    };
  }

  private Language map(Record record) {
    return new Language(
        record.get(UUID_ALIAS, UUID.class),
        record.get(NAME_ALIAS, String.class),
        record.get(CODE_ALIAS, String.class),
        record.get(NATIVE_NAME_ALIAS, String.class)
    );
  }


}
