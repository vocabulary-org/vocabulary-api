package org.enricogiurin.vocabulary.api.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;

import com.yourrents.services.common.searchable.FilterCriteria;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class LanguageRepositoryTest {

  static final int IT_ID = 20;
  @Autowired
  LanguageRepository languageRepository;


  @Test
  void findAll() {
    Page<Language> result = languageRepository.find(FilterCriteria.of(),
        PageRequest.ofSize(Integer.MAX_VALUE));
    assertThat(result, iterableWithSize(20));
  }

  @Test
  void findById() {
    Language result = languageRepository.findById(IT_ID).orElseThrow();
    assertThat(result, notNullValue());
    assertThat(result.uuid(), notNullValue());
    assertThat(result.name(), equalTo("Italian"));
    assertThat(result.nativeName(), equalTo("Italiano"));
  }


}