package org.enricogiurin.vocabulary.api.util;

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

import static org.enricogiurin.vocabulary.api.mapper.WordMapper.SENTENCE_ALIAS;
import static org.enricogiurin.vocabulary.api.mapper.WordMapper.TRANSLATION_ALIAS;
import static org.enricogiurin.vocabulary.api.mapper.WordMapper.UUID_ALIAS;
import static org.enricogiurin.vocabulary.jooq.tables.Word.WORD;

import com.yourrents.services.common.searchable.Searchable;
import com.yourrents.services.common.util.jooq.JooqUtils;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.mapper.AMapper;
import org.enricogiurin.vocabulary.api.model.Word;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Select;
import org.jooq.SelectJoinStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FindPageable<R extends Record> {
  private final JooqUtils jooqUtils;
  private final AMapper<Word> wordMapper;
  private final DSLContext dsl;

  //TODO - work in progress trying to have a "generic" component

  public Page<Word> find(Searchable filter, Pageable pageable) {
    Select<?> result = jooqUtils.paginate(
        dsl,
        jooqUtils.getQueryWithConditionsAndSorts(getSelect(),
            filter, this::getSupportedField,
            pageable, this::getSupportedField),
        pageable.getPageSize(), pageable.getOffset());

    List<Word> words = result.fetch(wordMapper::map);
    int totalRows = Objects.requireNonNullElse(
        result.fetchAny("total_rows", Integer.class), 0);
    return new PageImpl<>(words, pageable, totalRows);
  }

  private Field<?> getSupportedField(String field) {
    return switch (field) {
      case UUID_ALIAS -> WORD.EXTERNAL_ID;
      case SENTENCE_ALIAS-> WORD.SENTENCE;
      default -> throw new IllegalArgumentException(
          "Unexpected value for filter/sort field: " + field);
    };
  }

  private SelectJoinStep<Record3<UUID, String, String>> getSelect() {
    return dsl.select(
            WORD.EXTERNAL_ID.as(UUID_ALIAS),
            WORD.SENTENCE.as(SENTENCE_ALIAS),
            WORD.TRANSLATION.as(TRANSLATION_ALIAS))
        .from(WORD);
  }

}
