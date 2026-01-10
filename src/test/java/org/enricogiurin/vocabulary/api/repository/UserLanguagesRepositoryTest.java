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

import static org.assertj.core.api.Assertions.assertThat;

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.Language;
import org.enricogiurin.vocabulary.api.model.LanguageReference;
import org.enricogiurin.vocabulary.api.model.UserLanguages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class UserLanguagesRepositoryTest {

  private static final int USER_ID_WITH_USER_LANGUAGES = 1000000;
  private static final int USER_LANGUAGES_ID = 1000000;
  private static final int USER_ID_WITH_NO_USER_LANGUAGES = 1000001;
  @Autowired
  UserLanguagesRepository userLanguagesRepository;

  @Autowired
  LanguageRepository languageRepository;


  @Test
  void findById() {
    UserLanguages userLanguages = userLanguagesRepository.findById(USER_LANGUAGES_ID).orElseThrow();
    assertThat(userLanguages.language().name()).isEqualTo("Italian");
    assertThat(userLanguages.languageTo().name()).isEqualTo("English");
  }

  @Test
  void findByUserId() {
    UserLanguages userLanguages = userLanguagesRepository.findByUserId(USER_ID_WITH_USER_LANGUAGES)
        .orElseThrow();
    assertThat(userLanguages.language().name()).isEqualTo("Italian");
    assertThat(userLanguages.languageTo().name()).isEqualTo("English");
  }

  @Test
  void createUserLanguage() {
    assertThat(userLanguagesRepository.findByUserId(USER_ID_WITH_NO_USER_LANGUAGES))
        .isEmpty();

    Language english = languageRepository.findByName("English").orElseThrow();
    Language spanish = languageRepository.findByName("Spanish").orElseThrow();

    UserLanguages userLanguages = new UserLanguages(null,
        new LanguageReference(english.uuid(), null),
        new LanguageReference(spanish.uuid(), null));
    UserLanguages userLanguagesCreated = userLanguagesRepository.createOrUpdate(userLanguages,
        USER_ID_WITH_NO_USER_LANGUAGES);
    assertThat(userLanguagesCreated).isNotNull();
    assertThat(userLanguagesCreated.language().name()).isEqualTo("English");
    assertThat(userLanguagesCreated.languageTo().name()).isEqualTo("Spanish");
  }


  @Test
  void updateUserLanguage() {


    Language russian = languageRepository.findByName("Russian").orElseThrow();

    UserLanguages userLanguages = new UserLanguages(null,
        null,
        new LanguageReference(russian.uuid(), null));

    UserLanguages userLanguagesCreated = userLanguagesRepository.createOrUpdate(userLanguages,
        USER_ID_WITH_USER_LANGUAGES);
    assertThat(userLanguagesCreated).isNotNull();
    assertThat(userLanguagesCreated.language().name()).isEqualTo("Italian");
    assertThat(userLanguagesCreated.languageTo().name()).isEqualTo("Russian");

  }
}
