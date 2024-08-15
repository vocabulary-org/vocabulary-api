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
import static org.enricogiurin.vocabulary.jooq.Tables.USER;
import static org.enricogiurin.vocabulary.jooq.tables.Word.WORD;
import static org.jooq.Functions.nullOnAllNull;
import static org.jooq.impl.DSL.row;

import com.yourrents.services.common.searchable.Searchable;
import com.yourrents.services.common.util.exception.DataNotFoundException;
import com.yourrents.services.common.util.jooq.JooqUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.component.AuthenticatedUserProvider;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.model.Word;
import org.enricogiurin.vocabulary.api.model.view.LanguageView;
import org.enricogiurin.vocabulary.api.model.view.WordView;
import org.enricogiurin.vocabulary.jooq.tables.records.WordRecord;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record6;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
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
  public static final String SEARCH_LANGUAGE_NAME = "language.name";
  public static final String SEARCH_LANGUAGE_TO_NAME = "language.nameTo";


  private final DSLContext dsl;
  private final JooqUtils jooqUtils;
  private final LanguageRepository languageRepository;
  private final AuthenticatedUserProvider authenticatedUserProvider;
  private final UserRepository userRepository;

  public Optional<WordView> findByExternalId(UUID externalId) {

    return getSelect()
        .and(WORD.EXTERNAL_ID.eq(externalId))
        .fetchOptional()
        .map(this::map);
  }

  public Optional<WordView> findById(Integer id) {
    return getSelect()
        .and(WORD.ID.eq(id))
        .fetchOptional()
        .map(this::map);
  }

  public Page<WordView> find(Searchable filter, Pageable pageable) {
    Select<?> result = jooqUtils.paginate(
        dsl,
        jooqUtils.getQueryWithConditionsAndSorts(getSelect(),
            filter, this::getSupportedField,
            pageable, this::getSupportedField),
        pageable.getPageSize(), pageable.getOffset());

    List<WordView> words = result.fetch(this::map);
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
  public WordView create(Word word) {

    Integer userIdByAuthenticatedEmail = userRepository.findUserIdByAuthenticatedEmail();
    WordRecord wordRecord = dsl.newRecord(WORD);
    Integer languageIdByUuid = languageRepository.findLanguageIdByUuid(word.languageUuid());
    Integer languageToIdByUuid = languageRepository.findLanguageIdByUuid(word.languageToUuid());
    wordRecord.setLanguageId(languageIdByUuid);
    wordRecord.setLanguageToId(languageToIdByUuid);
    wordRecord.setSentence(word.sentence());
    wordRecord.setTranslation(word.translation());
    wordRecord.setDescription(word.description());
    wordRecord.setUserId(userIdByAuthenticatedEmail);
    wordRecord.setCreatedAt(LocalDateTime.now());
    wordRecord.insert();
    return findById(wordRecord.getId()).orElseThrow(
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
  public WordView update(UUID uuid, Word word) {
    WordRecord wordRecord = dsl.selectFrom(WORD)
        .where(WORD.EXTERNAL_ID.eq(uuid))
        .fetchOptional()
        .orElseThrow(() -> new DataNotFoundException("Word not found: " + uuid));
    if (word.languageUuid() != null) {
      Integer languageIdByUuid = languageRepository.findLanguageIdByUuid(word.languageUuid());
      wordRecord.setLanguageId(languageIdByUuid);
    }
    if (word.languageToUuid() != null) {
      Integer languageIdByUuid = languageRepository.findLanguageIdByUuid(word.languageToUuid());
      wordRecord.setLanguageToId(languageIdByUuid);
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
    wordRecord.setUpdatedAt(LocalDateTime.now());
    wordRecord.update();
    return findById(wordRecord.getId()).orElseThrow(
        () -> new DataExecutionException("failed to update word: " + uuid));
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


  private SelectConditionStep<Record6<UUID, String, String, String, LanguageView, LanguageView>> getSelect() {
    final String authenticatedUserEmail = authenticatedUserProvider.getAuthenticatedUserEmail();
    log.info("authenticated user: {}", authenticatedUserEmail);
    return dsl.select(
            WORD.EXTERNAL_ID.as(UUID_ALIAS),
            WORD.SENTENCE.as(SENTENCE_ALIAS),
            WORD.TRANSLATION.as(TRANSLATION_ALIAS),
            WORD.DESCRIPTION.as(DESCRIPTION_ALIAS),
            row(WORD.fkWordLanguage().NAME, WORD.fkWordLanguage().NATIVE_NAME)
                .mapping(nullOnAllNull(LanguageView::new)).as(LANGUAGE_ALIAS),
            row(WORD.fkWordLanguageTo().NAME, WORD.fkWordLanguageTo().NATIVE_NAME)
                .mapping(nullOnAllNull(LanguageView::new)).as(LANGUAGE_TO_ALIAS)
        )
        .from(WORD)
        .leftJoin(LANGUAGE).on(WORD.LANGUAGE_ID.eq(LANGUAGE.ID))
        .join(USER).on(WORD.USER_ID.eq(USER.ID))
        .where(USER.EMAIL.eq(authenticatedUserEmail));


  }

  private Field<?> getSupportedField(String field) {
    return switch (field) {
      case UUID_ALIAS -> WORD.EXTERNAL_ID;
      case SENTENCE_ALIAS -> WORD.SENTENCE;
      case TRANSLATION_ALIAS -> WORD.TRANSLATION;
      case SEARCH_LANGUAGE_NAME -> WORD.fkWordLanguage().NAME;
      case SEARCH_LANGUAGE_TO_NAME -> WORD.fkWordLanguageTo().NAME;
      default -> throw new IllegalArgumentException(
          "Unexpected value for filter/sort field: " + field);
    };
  }

  private WordView map(Record record) {
    return new WordView(
        record.get(UUID_ALIAS, UUID.class),
        record.get(SENTENCE_ALIAS, String.class),
        record.get(TRANSLATION_ALIAS, String.class),
        record.get(DESCRIPTION_ALIAS, String.class),
        record.get(LANGUAGE_ALIAS, LanguageView.class),
        record.get(LANGUAGE_TO_ALIAS, LanguageView.class)
    );
  }


}
