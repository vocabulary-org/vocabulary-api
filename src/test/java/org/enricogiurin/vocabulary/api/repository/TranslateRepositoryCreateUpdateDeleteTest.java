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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;

import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.model.Language;
import org.enricogiurin.vocabulary.api.model.Translation;
import org.enricogiurin.vocabulary.api.model.view.TranslationView;
import org.enricogiurin.vocabulary.api.model.view.WordView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class TranslateRepositoryCreateUpdateDeleteTest {

  @Autowired
  WordRepository wordRepository;

  @Autowired
  TranslateRepository translateRepository;

  @Autowired
  LanguageRepository languageRepository;

  @Test
  void create() {
    Language de = languageRepository.findById(11).orElseThrow();
    WordView cat = wordRepository.findById(1000002).orElseThrow();
    Translation dieKatze = new Translation(null, "die Katze", de.uuid(), cat.uuid());
    TranslationView result = translateRepository.create(dieKatze);
    assertThat(result, notNullValue());
    assertThat(result.uuid(), notNullValue());
    assertThat(result.content(), equalTo("die Katze"));
    assertThat(result.language().name(), equalTo("German"));
    assertThat(result.word().language().name(), equalTo("English"));
  }

  @Test
  void deleteAnExistingTranslation() {
    TranslationView translationView = translateRepository.findById(1000000).orElseThrow();
    boolean delete = translateRepository.delete(translationView.uuid());
    assertThat(delete, equalTo(true));
    Optional<TranslationView> optionalTranslationView = translateRepository.findById(1000000);
    assertThat(optionalTranslationView.isEmpty(), equalTo(true));
  }

  @Test
  void deleteANotExistingTranslation() {
    UUID randomUUID = UUID.randomUUID();
    DataNotFoundException ex = assertThrows(DataNotFoundException.class,
        () -> translateRepository.delete(randomUUID));
    assertThat(ex.getMessage(), equalTo("Translation not found: " + randomUUID));
  }

  @Test
  void update() {
    Language es = languageRepository.findById(4).orElseThrow();
    TranslationView salve = translateRepository.findById(1000000).orElseThrow();

    Translation translation = new Translation(null, "Hola", es.uuid(), null);
    TranslationView result = translateRepository.update(salve.uuid(), translation);
    assertThat(result, notNullValue());
    assertThat(result.uuid(), notNullValue());
    assertThat(result.content(), equalTo("Hola"));
    assertThat(result.language().name(), equalTo("Spanish"));
  }

  @Test
  void updateWithANotExistingLanguage() {
    UUID randomLanguageUUID = UUID.randomUUID();
    TranslationView salve = translateRepository.findById(1000000).orElseThrow();
    Translation translation = new Translation(null, "Hola", randomLanguageUUID, null);
    DataNotFoundException ex = assertThrows(DataNotFoundException.class,
        () -> translateRepository.update(salve.uuid(), translation));
    assertThat(ex.getMessage(), equalTo("Language not found: " + randomLanguageUUID));
  }
}
