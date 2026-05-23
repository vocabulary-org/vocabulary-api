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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.Article;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class PublicNounDeItRepositoryTest {

  @Autowired
  PublicNounDeItRepository repository;

  @Test
  void findRandom_returnsRequestedNumberOfNouns() {
    List<NounView> result = repository.findRandom(5);
    assertThat(result, hasSize(5));
  }

  @Test
  void findRandom_allFieldsPopulated() {
    List<NounView> result = repository.findRandom(10);
    assertThat(result, everyItem(notNullValue()));
    result.forEach(n -> {
      assertThat(n.uuid(), notNullValue());
      assertThat(n.wordDe(), notNullValue());
      assertThat(n.article(), notNullValue());
    });
  }

  @Test
  void findRandom_articleIsValidEnum() {
    List<NounView> result = repository.findRandom(20);
    result.forEach(n ->
        assertThat(Article.lookupLiteral(n.article().getLiteral()), notNullValue()));
  }
}
