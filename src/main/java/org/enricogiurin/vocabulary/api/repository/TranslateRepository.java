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
import static org.enricogiurin.vocabulary.jooq.Tables.TRANSLATION;
import static org.enricogiurin.vocabulary.jooq.tables.Word.WORD;
import static org.jooq.Functions.nullOnAllNull;
import static org.jooq.impl.DSL.row;

import com.yourrents.services.common.searchable.Searchable;
import com.yourrents.services.common.util.jooq.JooqUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.model.view.LanguageView;
import org.enricogiurin.vocabulary.api.model.view.TranslationView;
import org.enricogiurin.vocabulary.api.model.view.WordView;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.Select;
import org.jooq.SelectOnConditionStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TranslateRepository {

  static final String UUID_ALIAS = "uuid";
  static final String CONTENT_ALIAS = "content";
  static final String LANGUAGE_ALIAS = "language";
  static final String WORD_ALIAS = "word";


  private final DSLContext dsl;
  private final JooqUtils jooqUtils;


  public Optional<TranslationView> findById(Integer id) {
    return getSelect()
        .where(TRANSLATION.ID.eq(id))
        .fetchOptional()
        .map(this::map);
  }

  public Optional<TranslationView> findByExternalId(UUID externalId) {
    return getSelect()
        .where(TRANSLATION.EXTERNAL_ID.eq(externalId))
        .fetchOptional()
        .map(this::map);
  }

  public Page<TranslationView> find(Searchable filter, Pageable pageable) {
    Select<?> result = jooqUtils.paginate(
        dsl,
        jooqUtils.getQueryWithConditionsAndSorts(getSelect(),
            filter, this::getSupportedField,
            pageable, this::getSupportedField),
        pageable.getPageSize(), pageable.getOffset());

    List<TranslationView> translations = result.fetch(this::map);
    int totalRows = Objects.requireNonNullElse(
        result.fetchAny("total_rows", Integer.class), 0);
    return new PageImpl<>(translations, pageable, totalRows);
  }

  private SelectOnConditionStep<Record4<UUID, String, LanguageView, WordView>> getSelect() {
    return dsl.select(
            TRANSLATION.EXTERNAL_ID.as(UUID_ALIAS),
            TRANSLATION.TRANSLATION_CONTENT.as(CONTENT_ALIAS),
            row(TRANSLATION.language().NAME,
                TRANSLATION.language().NATIVE_NAME)
                .mapping(nullOnAllNull(LanguageView::new)).as(LANGUAGE_ALIAS),
            row(TRANSLATION.word().EXTERNAL_ID, TRANSLATION.word().SENTENCE,
                row(TRANSLATION.word().language().NAME,
                    TRANSLATION.word().language().NATIVE_NAME)
                    .mapping(nullOnAllNull(LanguageView::new)).as(LANGUAGE_ALIAS))
                .mapping(nullOnAllNull(WordView::new)).as(WORD_ALIAS)

        )
        .from(TRANSLATION)
        .leftJoin(LANGUAGE).on(TRANSLATION.LANGUAGE_ID.eq(LANGUAGE.ID))
        .leftJoin(WORD).on(TRANSLATION.WORD_ID.eq(WORD.ID));

  }


  private Field<?> getSupportedField(String field) {
    return switch (field) {
      case UUID_ALIAS -> TRANSLATION.EXTERNAL_ID;
      case CONTENT_ALIAS -> TRANSLATION.TRANSLATION_CONTENT;
      case LANGUAGE_ALIAS -> TRANSLATION.language().NAME;
      default -> throw new IllegalArgumentException(
          "Unexpected value for filter/sort field: " + field);
    };
  }

  private TranslationView map(Record record) {
    return new TranslationView(
        record.get(UUID_ALIAS, UUID.class),
        record.get(CONTENT_ALIAS, String.class),
        record.get(LANGUAGE_ALIAS, LanguageView.class),
        record.get(WORD_ALIAS, WordView.class)
    );
  }


}
