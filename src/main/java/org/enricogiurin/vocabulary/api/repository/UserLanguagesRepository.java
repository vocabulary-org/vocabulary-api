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
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.USER_LANGUAGES;
import static org.jooq.Functions.nullOnAllNull;
import static org.jooq.impl.DSL.row;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.exception.DataNotFoundException;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.tables.records.UserLanguagesRecord;
import org.enricogiurin.vocabulary.api.model.LanguageReference;
import org.enricogiurin.vocabulary.api.model.UserLanguages;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record3;
import org.jooq.SelectConditionStep;
import org.jooq.SelectOnConditionStep;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class UserLanguagesRepository {

  public static final String UUID_ALIAS = "uuid";
  public static final String LANGUAGE_ALIAS = "language";
  public static final String LANGUAGE_TO_ALIAS = "languageTo";

  private final DSLContext dsl;

  public Optional<UserLanguages> findByUserId(Integer userId) {
    return getSelect(userId)
        .fetchOptional()
        .map(this::map);
  }

  public Optional<UserLanguages> findById(Integer id) {
    return select()
        .where(USER_LANGUAGES.ID.eq(id))
        .fetchOptional()
        .map(this::map);
  }

  @Transactional(readOnly = false)
  public UserLanguages create(UserLanguages userLanguages, Integer userId) {
    UserLanguagesRecord userLanguagesRecord = dsl.newRecord(USER_LANGUAGES);
    userLanguagesRecord.setUserId(userId);
    Integer languageId = getLanguageId(userLanguages.language());
    userLanguagesRecord.setLanguageId(languageId);
    Integer languageToId = getLanguageId(userLanguages.languageTo());
    userLanguagesRecord.setLanguageToId(languageToId);
    userLanguagesRecord.insert();
    return findById(userLanguagesRecord.getId()).orElseThrow(
        () -> new DataExecutionException("failed to create userLanguages "));
  }

  @Transactional(readOnly = false)
  public UserLanguages update(UserLanguages userLanguages, UUID uuid) {
    UserLanguagesRecord userLanguagesRecord = dsl.selectFrom(USER_LANGUAGES)
        .where(USER_LANGUAGES.EXTERNAL_ID.eq(uuid))
        .fetchOptional()
        .orElseThrow(() -> new DataNotFoundException("record not found - uuid: " + uuid));
    if (userLanguages.language() != null && userLanguages.language().uuid() != null) {
      Integer languageId = getLanguageId(userLanguages.language());
      userLanguagesRecord.setLanguageId(languageId);
    }
    if (userLanguages.languageTo() != null && userLanguages.languageTo().uuid() != null) {
      Integer languageToId = getLanguageId(userLanguages.languageTo());
      userLanguagesRecord.setLanguageToId(languageToId);
    }
    userLanguagesRecord.update();
    return findById(userLanguagesRecord.getId()).orElseThrow(
        () -> new DataExecutionException("failed to update userLanguages"));
  }

  private SelectOnConditionStep<Record3<UUID, LanguageReference, LanguageReference>> select() {
    var L_FROM = LANGUAGE.as("l_from");
    var L_TO = LANGUAGE.as("l_to");
    return dsl.select(
            USER_LANGUAGES.EXTERNAL_ID.as(UUID_ALIAS),
            row(L_FROM.EXTERNAL_ID, L_FROM.NAME)
                .mapping(nullOnAllNull(LanguageReference::new)).as("language"),
            row(L_TO.EXTERNAL_ID, L_TO.NAME)
                .mapping(nullOnAllNull(LanguageReference::new)).as("languageTo")
        )
        .from(USER_LANGUAGES)
        .join(L_FROM).on(L_FROM.ID.eq(USER_LANGUAGES.LANGUAGE_ID))
        .join(L_TO).on(L_TO.ID.eq(USER_LANGUAGES.LANGUAGE_TO_ID));
  }

  private SelectConditionStep<Record3<UUID, LanguageReference, LanguageReference>> getSelect(
      Integer userId) {
    return select()
        .join(USER).on(USER.ID.eq(USER_LANGUAGES.USER_ID))
        .where(USER.ID.eq(userId));
  }

  private UserLanguages map(Record record) {
    return new UserLanguages(
        record.get(UUID_ALIAS, UUID.class),
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
