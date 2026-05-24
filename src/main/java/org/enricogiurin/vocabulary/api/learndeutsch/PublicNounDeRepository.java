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

import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.PUBLIC_NOUN_DE;
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.PUBLIC_NOUN_DE_EXAMPLE;
import static org.enricogiurin.vocabulary.api.jooq.vocabulary.Tables.PUBLIC_NOUN_DE_TRANSLATION;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicNounDeRepository {

  private final DSLContext dsl;

  public List<NounForGeneration> findWithMissingExamples(String lang, Integer limit) {
    var query = dsl.select(
            PUBLIC_NOUN_DE_TRANSLATION.ID,
            PUBLIC_NOUN_DE.ARTICLE,
            PUBLIC_NOUN_DE.WORD_DE,
            PUBLIC_NOUN_DE_TRANSLATION.TRANSLATION)
        .from(PUBLIC_NOUN_DE)
        .join(PUBLIC_NOUN_DE_TRANSLATION)
            .on(PUBLIC_NOUN_DE_TRANSLATION.NOUN_ID.eq(PUBLIC_NOUN_DE.ID)
                .and(PUBLIC_NOUN_DE_TRANSLATION.LANG.eq(lang))
                .and(PUBLIC_NOUN_DE_TRANSLATION.SORT_ORDER.eq(1)))
        .whereNotExists(
            dsl.selectOne()
                .from(PUBLIC_NOUN_DE_EXAMPLE)
                .where(PUBLIC_NOUN_DE_EXAMPLE.TRANSLATION_ID.eq(PUBLIC_NOUN_DE_TRANSLATION.ID)));
    return (limit != null ? query.limit(limit) : query).fetch(r -> new NounForGeneration(
        r.get(PUBLIC_NOUN_DE_TRANSLATION.ID),
        r.get(PUBLIC_NOUN_DE.ARTICLE),
        r.get(PUBLIC_NOUN_DE.WORD_DE),
        r.get(PUBLIC_NOUN_DE_TRANSLATION.TRANSLATION)));
  }

  @Transactional
  public void saveExample(int translationId, String sentenceDe, String sentenceTranslation) {
    dsl.insertInto(PUBLIC_NOUN_DE_EXAMPLE)
        .columns(
            PUBLIC_NOUN_DE_EXAMPLE.TRANSLATION_ID,
            PUBLIC_NOUN_DE_EXAMPLE.SENTENCE_DE,
            PUBLIC_NOUN_DE_EXAMPLE.SENTENCE_TRANSLATION)
        .values(translationId, sentenceDe, sentenceTranslation)
        .execute();
  }

  public List<NounView> findRandom(int limit, String lang) {
    var examples = DSL.multiset(
        DSL.select(
                PUBLIC_NOUN_DE_EXAMPLE.SENTENCE_DE,
                PUBLIC_NOUN_DE_EXAMPLE.SENTENCE_TRANSLATION)
            .from(PUBLIC_NOUN_DE_EXAMPLE)
            .where(PUBLIC_NOUN_DE_EXAMPLE.TRANSLATION_ID.eq(PUBLIC_NOUN_DE_TRANSLATION.ID))
    ).convertFrom(r -> r.map(e -> new ExampleView(
        e.get(PUBLIC_NOUN_DE_EXAMPLE.SENTENCE_DE),
        e.get(PUBLIC_NOUN_DE_EXAMPLE.SENTENCE_TRANSLATION)
    )));

    return dsl.select(
            PUBLIC_NOUN_DE.EXTERNAL_ID,
            PUBLIC_NOUN_DE.WORD_DE,
            PUBLIC_NOUN_DE.ARTICLE,
            PUBLIC_NOUN_DE.PLURAL_DE,
            PUBLIC_NOUN_DE.PLURAL_DISTRACTORS,
            PUBLIC_NOUN_DE_TRANSLATION.TRANSLATION,
            examples)
        .from(PUBLIC_NOUN_DE)
        .leftJoin(PUBLIC_NOUN_DE_TRANSLATION)
            .on(PUBLIC_NOUN_DE_TRANSLATION.NOUN_ID.eq(PUBLIC_NOUN_DE.ID)
                .and(PUBLIC_NOUN_DE_TRANSLATION.LANG.eq(lang))
                .and(PUBLIC_NOUN_DE_TRANSLATION.SORT_ORDER.eq(1)))
        .orderBy(DSL.rand())
        .limit(limit)
        .fetch(r -> new NounView(
            r.get(PUBLIC_NOUN_DE.EXTERNAL_ID),
            r.get(PUBLIC_NOUN_DE.WORD_DE),
            r.get(PUBLIC_NOUN_DE.ARTICLE),
            r.get(PUBLIC_NOUN_DE.PLURAL_DE),
            r.get(PUBLIC_NOUN_DE.PLURAL_DISTRACTORS),
            r.get(PUBLIC_NOUN_DE_TRANSLATION.TRANSLATION),
            r.get(examples)));
  }
}
