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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;

import com.yourrents.services.common.searchable.FilterCondition;
import com.yourrents.services.common.searchable.FilterCriteria;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.model.view.TranslationView;
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
class TranslateRepositoryTest {

  static final UUID HALLO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  static final UUID WORD_HELLO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final int HALLO_ID = 1000001;

  @Autowired
  TranslateRepository translateRepository;

  @Autowired
  WordRepository wordRepository;

  @Test
  void findById() {
    TranslationView result = translateRepository.findById(HALLO_ID).orElseThrow();
    assertThat(result, notNullValue());
    assertThat(result.uuid(), equalTo(HALLO_UUID));
    assertThat(result.language().name(), equalTo("German"));
    assertThat(result.word().sentence(), equalTo("Hello"));
    assertThat(result.word().language().nativeName(), equalTo("English"));
  }

  @Test
  void findByExternalId() {
    TranslationView result = translateRepository.findByExternalId(HALLO_UUID).orElseThrow();
    assertThat(result, notNullValue());
    assertThat(result.uuid(), equalTo(HALLO_UUID));
    assertThat(result.language().nativeName(), equalTo("Deutsch"));
  }

  @Test
  void findFilteredByContentsContainsIgnoreCaseWithOrderByLanguageAsc() {
    Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE,
        Sort.by(Order.asc(TranslateRepository.SEARCH_LANGUAGE_NAME)));
    FilterCriteria filter = FilterCriteria.of(
        FilterCondition.of(TranslateRepository.CONTENT_ALIAS, "containsIgnoreCase", "al"));
    Page<TranslationView> result = translateRepository.find(filter, pageable);
    assertThat(result, iterableWithSize(2));
    assertThat(result.getContent().getFirst().content(), equalTo("Hallo"));
  }

  @Test
  void findAllTranslationOfWord() {
    Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE,
        Sort.by(Order.asc(TranslateRepository.CONTENT_ALIAS)));
    FilterCriteria filter = FilterCriteria.of(
        FilterCondition.of(TranslateRepository.SEARCH_WORD_UUID, "eq", WORD_HELLO_UUID));
    Page<TranslationView> result = translateRepository.find(filter, pageable);
    assertThat(result, iterableWithSize(2));
    assertThat(result.getContent().getFirst().content(), equalTo("Hallo"));
  }


}
