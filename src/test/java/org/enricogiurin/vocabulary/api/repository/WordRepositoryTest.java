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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;

import com.yourrents.services.common.searchable.FilterCondition;
import com.yourrents.services.common.searchable.FilterCriteria;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.jooq.CustomJooqUtils;
import org.enricogiurin.vocabulary.api.model.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class WordRepositoryTest {

  static final UUID HELLO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final int HELLO_ID = 1000000;
  static final int USER_ENRICO_ID = 1000000;
  static final int USER_LUCIO_ID = 1000001;

  @Autowired
  WordRepository wordRepository;

  @BeforeEach
  void setUp() {

  }

  @Test
  void findByExternalId() {
    Word word = wordRepository.findByExternalId(
        HELLO_UUID, USER_ENRICO_ID).orElseThrow();
    assertThat(word, notNullValue());
    assertThat(word.sentence(), equalTo("Hello"));
    assertThat(word.language().getLanguage(), equalTo("English"));

  }

  @Test
  void findById() {
    Word word = wordRepository.findById(HELLO_ID, USER_ENRICO_ID).orElseThrow();
    assertThat(word, notNullValue());
    assertThat(word.uuid(), equalTo(HELLO_UUID));
    assertThat(word.language().getLanguage(), equalTo("English"));
  }

  @Test
  void findAllWordsOwnedByEnrico() {
    Page<Word> result = wordRepository.find(FilterCriteria.of(),
        PageRequest.ofSize(Integer.MAX_VALUE), USER_ENRICO_ID);
    assertThat(result, iterableWithSize(5));
  }

  @Test
  void findAllWordsOwnedByLucio() {
    Page<Word> result = wordRepository.find(FilterCriteria.of(),
        PageRequest.ofSize(Integer.MAX_VALUE), USER_LUCIO_ID);
    assertThat(result, iterableWithSize(1));
  }

  @Test
  void findByNameContaining() {
    Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Order.asc("sentence")));
    FilterCriteria filter = FilterCriteria.of(
        FilterCondition.of("sentence", CustomJooqUtils.CONTAINS_IGNORE_CASE, "cat"));
    Page<Word> page = wordRepository.find(filter, pageable, USER_ENRICO_ID);
    assertThat(page, iterableWithSize(2));
    Word word = page.getContent().getFirst();
    assertThat(word, notNullValue());
    assertThat(word.sentence(), equalTo("cat"));
  }

  @Test
  void findByLanguageEq() {
    Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Order.asc("sentence")));
    FilterCriteria filter = FilterCriteria.of(
        FilterCondition.of(WordRepository.LANGUAGE_TO_ALIAS, CustomJooqUtils.EQUAL, "German"));
    Page<Word> page = wordRepository.find(filter, pageable, USER_ENRICO_ID);
    assertThat(page, iterableWithSize(1));
    Word word = page.getContent().getFirst();
    assertThat(word, notNullValue());
    assertThat(word.sentence(), equalTo("Latte"));
  }


}
