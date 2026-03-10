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

import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.TAG;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.model.Tag;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TagRepository {

  private final DSLContext dsl;

  public List<Tag> findAll() {
    return dsl.select(TAG.NAME, TAG.DESCRIPTION)
        .from(TAG)
        .orderBy(TAG.NAME)
        .fetch(r -> new Tag(r.get(TAG.NAME), r.get(TAG.DESCRIPTION)));
  }
}
