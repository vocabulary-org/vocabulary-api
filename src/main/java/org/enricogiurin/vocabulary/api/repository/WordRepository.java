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


import static org.enricogiurin.vocabulary.api.mapper.WordMapper.SENTENCE_ALIAS;
import static org.enricogiurin.vocabulary.api.mapper.WordMapper.UUID_ALIAS;
import static org.enricogiurin.vocabulary.jooq.tables.Word.WORD;

import com.yourrents.services.common.searchable.Searchable;
import com.yourrents.services.common.util.exception.DataNotFoundException;
import com.yourrents.services.common.util.jooq.JooqUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.mapper.WordMapper;
import org.enricogiurin.vocabulary.api.model.Word;
import org.enricogiurin.vocabulary.jooq.tables.records.WordRecord;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
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
public class WordRepository {

  //TODO - fix this, we just use language by default
  static Integer DEFAULT_LANGUAGE_ID = 1;


  private final DSLContext dsl;
  private final JooqUtils jooqUtils;
  private final WordMapper wordMapper;
  private final LanguageRepository languageRepository;

  public Optional<Word> findByExternalId(UUID externalId) {
    return getSelect()
        .where(WORD.EXTERNAL_ID.eq(externalId))
        .fetchOptional()
        .map(wordMapper::map);
  }

  public Optional<Word> findById(Integer id) {
    return getSelect()
        .where(WORD.ID.eq(id))
        .fetchOptional()
        .map(wordMapper::map);
  }

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

  /**
   * Create a new Word.
   * @return the new created Word
   * @throws DataExecutionException if something unexpected happens
   */
  @Transactional(readOnly = false)
  public Word create(Word word) {
    WordRecord wordRecord = dsl.newRecord(WORD);
    wordRecord.setSentence(word.sentence());
    //TODO  - to fix this
    wordRecord.setLanguageId(DEFAULT_LANGUAGE_ID);
    wordRecord.insert();
    return findById(wordRecord.getId()).orElseThrow(
        () -> new DataExecutionException("failed to create word[sentence]: " + word.sentence()));
  }

  /**
   * Delete a word
   *
   * @return true if the word has been deleted, false otherwise
   * @throws DataNotFoundException if the city does not exist
   */
  @Transactional(readOnly = false)
  public boolean delete(UUID uuid) {
    Integer propertyId = dsl.select(WORD.ID)
        .from(WORD)
        .where(WORD.EXTERNAL_ID.eq(uuid))
        .fetchOptional(WORD.ID).orElseThrow(
            () -> new DataNotFoundException("Word not found: " + uuid));
    return dsl.deleteFrom(WORD)
        .where(WORD.ID.eq(propertyId))
        .execute() > 0;
  }

  /**
   * Update a word.
   * <p>
   * You can update the sentence, the translation. You can't update the word uuid.
   * <p>
   * Only not null fields are used to update the word.
   *
   * @param uuid the uuid of the word to update
   * @param word the data of word to update.
   * @return the updated word
   * @throws DataNotFoundException  if the word does not exist
   * @throws DataExecutionException if something unexpected happens
   */
  @Transactional(readOnly = false)
  public Word update(UUID uuid, Word word) {
    WordRecord wordRecord = dsl.selectFrom(WORD)
        .where(WORD.EXTERNAL_ID.eq(uuid))
        .fetchOptional()
        .orElseThrow(() -> new DataNotFoundException("Word not found: " + uuid));
    if (word.sentence() != null) {
      wordRecord.setSentence(word.sentence());
    }
    wordRecord.update();
    return findById(wordRecord.getId()).orElseThrow(
        () -> new DataExecutionException("failed to update word: " + uuid));
  }

  private SelectJoinStep<Record2<UUID, String>> getSelect() {
    return dsl.select(
            WORD.EXTERNAL_ID.as(UUID_ALIAS),
            WORD.SENTENCE.as(SENTENCE_ALIAS))
        //WORD.TRANSLATION.as(TRANSLATION_ALIAS))
        .from(WORD);
  }

  private Field<?> getSupportedField(String field) {
    return switch (field) {
      case UUID_ALIAS -> WORD.EXTERNAL_ID;
      case SENTENCE_ALIAS-> WORD.SENTENCE;
      default -> throw new IllegalArgumentException(
          "Unexpected value for filter/sort field: " + field);
    };
  }

}
