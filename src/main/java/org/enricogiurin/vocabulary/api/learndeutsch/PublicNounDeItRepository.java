package org.enricogiurin.vocabulary.api.learndeutsch;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 - 2026 Vocabulary Team
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

import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.PUBLIC_NOUN_DE_IT;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicNounDeItRepository {

  private final DSLContext dsl;

  public List<NounView> findRandom(int limit) {
    return dsl.select(
            PUBLIC_NOUN_DE_IT.EXTERNAL_ID,
            PUBLIC_NOUN_DE_IT.WORD_DE,
            PUBLIC_NOUN_DE_IT.ARTICLE,
            PUBLIC_NOUN_DE_IT.PLURAL_DE,
            PUBLIC_NOUN_DE_IT.PLURAL_DISTRACTORS)
        .from(PUBLIC_NOUN_DE_IT)
        .orderBy(DSL.rand())
        .limit(limit)
        .fetch(r -> new NounView(
            r.get(PUBLIC_NOUN_DE_IT.EXTERNAL_ID),
            r.get(PUBLIC_NOUN_DE_IT.WORD_DE),
            r.get(PUBLIC_NOUN_DE_IT.ARTICLE),
            r.get(PUBLIC_NOUN_DE_IT.PLURAL_DE),
            r.get(PUBLIC_NOUN_DE_IT.PLURAL_DISTRACTORS)));
  }
}
