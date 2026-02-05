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
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.USER;
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.tables.Word.WORD;
import static org.jooq.Functions.nullOnAllNull;
import static org.jooq.impl.DSL.row;

import com.yourrents.services.common.searchable.Searchable;
import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.jooq.CustomJooqUtils;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.tables.records.WordRecord;
import org.enricogiurin.vocabulary.api.model.LanguageReference;
import org.enricogiurin.vocabulary.api.model.Word;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record6;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectOnConditionStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WordRepository {

  public static final String UUID_ALIAS = "uuid";
  public static final String SENTENCE_ALIAS = "sentence";
  public static final String TRANSLATION_ALIAS = "translation";
  public static final String LANGUAGE_ALIAS = "language";
  public static final String LANGUAGE_TO_ALIAS = "languageTo";
  public static final String DESCRIPTION_ALIAS = "description";
  public static final String UPDATED_AT = "updatedAt";


  private final DSLContext dsl;
  private final CustomJooqUtils jooqUtils;

  public Optional<Word> findByExternalId(UUID externalId, Integer userId) {
    return getSelect(userId)
        .and(WORD.EXTERNAL_ID.eq(externalId))
        .fetchOptional()
        .map(this::map);
  }

  /**
   * Both wordId and userId are required to guarantee that the authenticated user can access only
   * words that belong to them.
   *
   * @param wordId
   * @param userId
   * @return
   */
  public Optional<Word> findById(Integer wordId, Integer userId) {
    return getSelect(userId)
        .and(WORD.ID.eq(wordId))
        .fetchOptional()
        .map(this::map);
  }

  public Page<Word> find(Searchable filter, Pageable pageable, Integer userId) {
    Select<?> result = jooqUtils.paginate(
        dsl,
        jooqUtils.getQueryWithConditionsAndSorts(getSelect(userId),
            filter, this::getSupportedField,
            pageable, this::getSupportedField),
        pageable.getPageSize(), pageable.getOffset());

    List<Word> words = result.fetch(this::map);
    int totalRows = Objects.requireNonNullElse(
        result.fetchAny("total_rows", Integer.class), 0);
    return new PageImpl<>(words, pageable, totalRows);
  }

  /**
   * Create a new Word.
   *
   * @return the new created Word
   * @throws DataExecutionException if something unexpected happens
   */
  @Transactional(readOnly = false)
  public Word create(Word word, Integer userId) {
    WordRecord wordRecord = dsl.newRecord(WORD);

    Integer languageId = getLanguageId(word.language());
    wordRecord.setLanguageId(languageId);

    Integer languageToId = getLanguageId(word.languageTo());
    wordRecord.setLanguageToId(languageToId);

    wordRecord.setSentence(word.sentence());
    wordRecord.setTranslation(word.translation());
    wordRecord.setDescription(word.description());
    wordRecord.setUserId(userId);
    wordRecord.insert();
    return findById(wordRecord.getId(), userId).orElseThrow(
        () -> new DataExecutionException("failed to create word[sentence]: " + word.sentence()));
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
   * @throws DataNotFoundException  if not null language, uuid does not exist
   * @throws DataExecutionException if something unexpected happens
   */
  @Transactional(readOnly = false)
  public Word update(UUID uuid, Word word, Integer userId) {
    WordRecord wordRecord = dsl.selectFrom(WORD)
        .where(WORD.EXTERNAL_ID.eq(uuid))
        .and(WORD.USER_ID.eq(userId))
        .fetchOptional()
        .orElseThrow(() -> new DataNotFoundException("Word not found: " + uuid));
    if (word.language() != null && word.language().uuid() != null) {
      Integer languageId = getLanguageId(word.language());
      wordRecord.setLanguageId(languageId);
    }
    if (word.languageTo() != null && word.languageTo().uuid() != null) {
      Integer languageToId = getLanguageId(word.languageTo());
      wordRecord.setLanguageToId(languageToId);
    }
    if (word.sentence() != null) {
      wordRecord.setSentence(word.sentence());
    }
    if (word.translation() != null) {
      wordRecord.setTranslation(word.translation());
    }
    if (word.description() != null) {
      wordRecord.setDescription(word.description());
    }
    wordRecord.setUpdatedAt(OffsetDateTime.now());
    wordRecord.update();
    return findById(wordRecord.getId(), userId).orElseThrow(
        () -> new DataExecutionException("failed to update word: " + uuid));
  }

  /**
   * Delete a word
   *
   * @return true if the word has been deleted, false otherwise
   * @throws DataNotFoundException if the city does not exist
   */
  @Transactional(readOnly = false)
  public boolean delete(UUID uuid, Integer userId) {
    Integer propertyId = dsl.select(WORD.ID)
        .from(WORD)
        .where(WORD.EXTERNAL_ID.eq(uuid))
        .and(WORD.USER_ID.eq(userId))
        .fetchOptional(WORD.ID).orElseThrow(
            () -> new DataNotFoundException("Word not found: " + uuid));
    return dsl.deleteFrom(WORD)
        .where(WORD.ID.eq(propertyId))
        .execute() > 0;
  }


  private SelectConditionStep<Record6<UUID, String, String, String, LanguageReference, LanguageReference>> getSelect(
      Integer userId) {
    return select()
        .join(USER).on(USER.ID.eq(WORD.USER_ID))
        .where(WORD.USER_ID.eq(userId));
  }


  //code fixed via CGPT
  private SelectOnConditionStep<Record6<UUID, String, String, String, LanguageReference, LanguageReference>> select() {
    var L_FROM = LANGUAGE.as("l_from");
    var L_TO = LANGUAGE.as("l_to");
    return dsl.select(
            WORD.EXTERNAL_ID.as(UUID_ALIAS),
            WORD.SENTENCE.as(SENTENCE_ALIAS),
            WORD.TRANSLATION.as(TRANSLATION_ALIAS),
            WORD.DESCRIPTION.as(DESCRIPTION_ALIAS),
            row(L_FROM.EXTERNAL_ID, L_FROM.NAME)
                .mapping(nullOnAllNull(LanguageReference::new)).as("language"),
            row(L_TO.EXTERNAL_ID, L_TO.NAME)
                .mapping(nullOnAllNull(LanguageReference::new)).as("languageTo")
        )
        .from(WORD)
        .join(L_FROM).on(L_FROM.ID.eq(WORD.LANGUAGE_ID))
        .join(L_TO).on(L_TO.ID.eq(WORD.LANGUAGE_TO_ID));
  }
  private Field<?> getSupportedField(String field) {
    return switch (field) {
      case UUID_ALIAS -> WORD.EXTERNAL_ID;
      case SENTENCE_ALIAS -> WORD.SENTENCE;
      case TRANSLATION_ALIAS -> WORD.TRANSLATION;
      case LANGUAGE_ALIAS -> WORD.fkWordLanguage().NAME;
      case LANGUAGE_TO_ALIAS -> WORD.fkWordLanguageTo().NAME;
      case UPDATED_AT -> WORD.UPDATED_AT;

      default -> throw new IllegalArgumentException(
          "Unexpected value for filter/sort field: " + field);
    };
  }

  private Word map(Record record) {
    return new Word(
        record.get(UUID_ALIAS, UUID.class),
        record.get(SENTENCE_ALIAS, String.class),
        record.get(TRANSLATION_ALIAS, String.class),
        record.get(DESCRIPTION_ALIAS, String.class),
        record.get(LANGUAGE_ALIAS, LanguageReference.class),
        record.get(LANGUAGE_TO_ALIAS, LanguageReference.class)
    );
  }

  private Integer getLanguageId(LanguageReference language) {
    return dsl.select(LANGUAGE.ID)
        .from(LANGUAGE)
        .where(LANGUAGE.EXTERNAL_ID.eq(language.uuid()))
        .fetchOptional(LANGUAGE.ID).orElseThrow(
            () -> new IllegalArgumentException("Language not found: "
                + language.uuid()));
  }


}
