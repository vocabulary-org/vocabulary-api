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


import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.USER;

import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.tables.records.UserRecord;
import org.enricogiurin.vocabulary.api.model.User;
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
public class UserRepository {

  public static final String UUID_ALIAS = "uuid";
  public static final String USERNAME_ALIAS = "username";
  public static final String EMAIL_ALIAS = "email";
  public static final String IS_ADMIN_ALIAS = "isAdmin";
  private final DSLContext dsl;


  public Optional<User> findById(Integer id) {
    return getSelect()
        .where(USER.ID.eq(id))
        .fetchOptional()
        .map(this::map);
  }

  public Integer findUserIdByKeycloakId(String keycloakId) {
    return dsl.select(USER.ID)
        .from(USER)
        .where(USER.KEYCLOAKID.eq(keycloakId))
        .fetchOptional(USER.ID)
        .orElseThrow(
            () -> new DataNotFoundException("Cannot find user with keycloakId: " + keycloakId));
  }


  public Optional<User> findByUuid(UUID uuid) {
    return getSelect()
        .where(USER.EXTERNAL_ID.eq(uuid))
        .fetchOptional()
        .map(this::map);
  }

  public Optional<User> findByEmail(String email) {
    return getSelect()
        .where(USER.EMAIL.eq(email))
        .fetchOptional()
        .map(this::map);
  }

  public Integer findIdByUuid(UUID uuid) {

    return dsl.select(USER.ID)
        .from(USER)
        .where(USER.EXTERNAL_ID.eq(uuid))
        .fetchOptional(USER.ID).orElseThrow(
            () -> new DataNotFoundException("User not found: "
                + uuid));
  }


  /**
   * Create a new User.
   *
   * @return the new created User
   * @throws DataExecutionException if something unexpected happens
   */
  //@Transactional(readOnly = false)
  public User add(User user) {
    Optional<User> optionalUser = findByEmail(user.email());
    if (optionalUser.isPresent()) {
      throw new IllegalArgumentException(user.email() + " is already present in the User table");
    }
    UserRecord userRecord = dsl.newRecord(USER);
    userRecord.setUsername(user.username());
    userRecord.setEmail(user.email());
    userRecord.insert();
    return findById(userRecord.getId()).orElseThrow(
        () -> new DataExecutionException("failed to create user[username]: " + user.username()));
  }


  /**
   * Update an existing user
   *
   * @return the new created User
   * @throws DataExecutionException if something unexpected happens
   */
  @Transactional(readOnly = false)
  public User update(UUID uuid, User user) {
    UserRecord userRecord = dsl.selectFrom(USER)
        .where(USER.EXTERNAL_ID.eq(uuid))
        .fetchOptional().orElseThrow(
            () -> new DataNotFoundException("User not found: " + uuid));

    userRecord.setUsername(user.username());
    userRecord.setEmail(user.email());
    userRecord.setIsAdmin(user.isAdmin());
    userRecord.insert();
    return findById(userRecord.getId()).orElseThrow(
        () -> new DataExecutionException("failed to update user[uuid]: " + uuid));
  }

  private SelectJoinStep<Record4<UUID, String, String, Boolean>> getSelect() {
    return dsl.select(
            USER.EXTERNAL_ID.as(UUID_ALIAS),
            USER.USERNAME.as(USERNAME_ALIAS),
            USER.EMAIL.as(EMAIL_ALIAS),
            USER.IS_ADMIN.as(IS_ADMIN_ALIAS))
        .from(USER);
  }

  private User map(Record record) {
    return new User(
        record.get(UUID_ALIAS, UUID.class),
        record.get(USERNAME_ALIAS, String.class),
        record.get(EMAIL_ALIAS, String.class),
        record.get(IS_ADMIN_ALIAS, Boolean.class)
    );
  }

}
