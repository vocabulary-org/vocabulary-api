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
import org.enricogiurin.vocabulary.api.model.User;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class should only be used by the
 * {@link org.enricogiurin.vocabulary.api.service.KeycloakClientService}
 */
@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class UserRepository {

  public static final String UUID_ALIAS = "uuid";
  public static final String USERNAME_ALIAS = "username";
  public static final String EMAIL_ALIAS = "email";
  public static final String KEYCLOAK_ID_ALIAS = "keycloakId";

  private final DSLContext dsl;


  public Optional<User> findById(Integer id) {
    return getSelect()
        .where(USER.ID.eq(id))
        .fetchOptional()
        .map(this::map);
  }

  public Optional<Integer> findUserIdByKeycloakId(String keycloakId) {
    return dsl.select(USER.ID)
        .from(USER)
        .where(USER.KEYCLOAKID.eq(keycloakId))
        .fetchOptional(USER.ID);
  }


  /**
   * Returns the userId for the given keycloakId, inserting the user if not already present.
   * Safe under concurrent requests: uses ON CONFLICT DO NOTHING on the insert.
   */
  @Transactional(readOnly = false)
  public InsertResult findOrInsert(String keycloakId, String username) {
    Optional<Integer> existing = findUserIdByKeycloakId(keycloakId);
    if (existing.isPresent()) {
      return new InsertResult(existing.get(), false);
    }
    int inserted = dsl.insertInto(USER)
        .set(USER.KEYCLOAKID, keycloakId)
        .set(USER.USERNAME, username)
        .onConflict(USER.KEYCLOAKID)
        .doNothing()
        .execute();
    if (inserted == 1) {
      log.info("created new user having keycloakId: {}", keycloakId);
    } else {
      log.info("concurrent insert detected, user already exists for keycloakId: {}", keycloakId);
    }
    Integer userId = findUserIdByKeycloakId(keycloakId)
        .orElseThrow(() -> new DataExecutionException("can't find user having keycloakId: " + keycloakId));
    return new InsertResult(userId, inserted == 1);
  }

  public record InsertResult(Integer userId, boolean created) {

  }


  /**
   * Delete the user
   *
   * @return true if the user has been deleted, false otherwise
   * @throws DataNotFoundException if the user does not exist
   */
  @Transactional(readOnly = false)
  public boolean delete(Integer userId) {
    return dsl.deleteFrom(USER)
        .where(USER.ID.eq(userId))
        .execute() > 0;
  }

  private SelectJoinStep<Record4<UUID, String, String, String>> getSelect() {
    return dsl.select(
            USER.EXTERNAL_ID.as(UUID_ALIAS),
            USER.USERNAME.as(USERNAME_ALIAS),
            USER.EMAIL.as(EMAIL_ALIAS),
            USER.KEYCLOAKID.as(KEYCLOAK_ID_ALIAS))
        .from(USER);
  }

  private User map(Record record) {
    return new User(
        record.get(UUID_ALIAS, UUID.class),
        record.get(USERNAME_ALIAS, String.class),
        record.get(EMAIL_ALIAS, String.class),
        record.get(KEYCLOAK_ID_ALIAS, String.class)
    );
  }

}
