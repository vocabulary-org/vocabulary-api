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

import static org.enricogiurin.vocabulary.jooq.Tables.USER;
import static org.enricogiurin.vocabulary.jooq.tables.Word.WORD;

import com.yourrents.services.common.searchable.Searchable;
import com.yourrents.services.common.util.jooq.JooqUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.model.User;
import org.enricogiurin.vocabulary.jooq.tables.records.UserRecord;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record3;
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
@Slf4j
public class UserRepository {

  public static final String UUID_ALIAS = "uuid";
  public static final String USERNAME_ALIAS = "username";
  public static final String EMAIL_ALIAS = "email";
  private final DSLContext dsl;
  private final JooqUtils jooqUtils;


  public Optional<User> findById(Integer id) {
    return getSelect()
        .where(WORD.ID.eq(id))
        .fetchOptional()
        .map(this::map);
  }

  public Optional<User> findByExternalId(UUID externalId) {
    return getSelect()
        .where(WORD.EXTERNAL_ID.eq(externalId))
        .fetchOptional()
        .map(this::map);
  }

  public Page<User> find(Searchable filter, Pageable pageable) {
    Select<?> result = jooqUtils.paginate(
        dsl,
        jooqUtils.getQueryWithConditionsAndSorts(getSelect(),
            filter, this::getSupportedField,
            pageable, this::getSupportedField),
        pageable.getPageSize(), pageable.getOffset());
    List<User> users = result.fetch(this::map);
    int totalRows = Objects.requireNonNullElse(
        result.fetchAny("total_rows", Integer.class), 0);
    return new PageImpl<>(users, pageable, totalRows);
  }

  /**
   * Create a new User.
   *
   * @return the new created User
   * @throws DataExecutionException if something unexpected happens
   */
  @Transactional(readOnly = false)
  public User create(User user) {
    UserRecord userRecord = dsl.newRecord(USER);
    userRecord.setUsername(user.username());
    userRecord.setEmail(user.email());
    userRecord.setCreatedAt(LocalDateTime.now());
    userRecord.insert();
    return findById(userRecord.getId()).orElseThrow(
        () -> new DataExecutionException("failed to create user[username]: " + user.username()));
  }

  private SelectJoinStep<Record3<UUID, String, String>> getSelect() {
    return dsl.select(
            USER.EXTERNAL_ID.as(UUID_ALIAS),
            USER.USERNAME.as(USERNAME_ALIAS),
            USER.EMAIL.as(EMAIL_ALIAS))
        .from(WORD);
  }

  private User map(Record record) {
    return new User(
        record.get(UUID_ALIAS, UUID.class),
        record.get(USERNAME_ALIAS, String.class),
        record.get(USERNAME_ALIAS, String.class)
    );
  }

  private Field<?> getSupportedField(String field) {
    return switch (field) {
      case UUID_ALIAS -> USER.EXTERNAL_ID;
      case USERNAME_ALIAS -> USER.USERNAME;
      case EMAIL_ALIAS -> USER.EMAIL;
      default -> throw new IllegalArgumentException(
          "Unexpected value for filter/sort field: " + field);
    };
  }

}
