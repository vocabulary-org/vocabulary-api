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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;

import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.Language;
import org.enricogiurin.vocabulary.api.model.Word;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@Transactional
class WordRepositoryCreateUpdateDeleteTest {

  static final int HELLO_ID = 1000000;
  static final int USER_ENRICO_ID = 1000000;

  @Autowired
  WordRepository wordRepository;


  @BeforeEach
  void setUp() {
  }

  @Test
  void createANewWord() {

    Word newWord = new Word(null, "dog", "der Hund", "my dog", Language.ENGLISH, Language.GERMAN);
    Word result = wordRepository.create(newWord, USER_ENRICO_ID);
    assertThat(result, notNullValue());
    assertThat(result.uuid(), notNullValue());
    assertThat(result.sentence(), equalTo("dog"));
    assertThat(result.translation(), equalTo("der Hund"));
    assertThat(result.description(), equalTo("my dog"));
    assertThat(result.language().getLanguage(), equalTo("English"));
    assertThat(result.languageTo().getLanguage(), equalTo("German"));

  }

  @Test
  void deleteAnExistingWord() {
    Word word = wordRepository.findById(HELLO_ID, USER_ENRICO_ID).orElseThrow();
    boolean delete = wordRepository.delete(word.uuid(), USER_ENRICO_ID);
    assertThat(delete, equalTo(true));
    Optional<Word> wordOptional = wordRepository.findById(HELLO_ID, USER_ENRICO_ID);
    assertThat(wordOptional.isEmpty(), equalTo(true));
  }

  /*
  javadoc: public static UUID randomUUID()
  Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
  The UUID is generated using a cryptographically strong pseudo random number generator.
  */
  @Test
  void deleteANotExistingWord() {
    UUID randomUUID = UUID.randomUUID();
    DataNotFoundException ex = assertThrows(DataNotFoundException.class,
        () -> wordRepository.delete(randomUUID, USER_ENRICO_ID));
    assertThat(ex.getMessage(), equalTo("Word not found: " + randomUUID));
  }

  @Test
  void updateAnExistingWord() {
    Word word = wordRepository.findById(HELLO_ID, USER_ENRICO_ID).orElseThrow();
    Word updateWord = new Word(null, null, "new translation", "new description", null, null);
    Word result = wordRepository.update(word.uuid(), updateWord, USER_ENRICO_ID);
    assertThat(result, notNullValue());
    assertThat(result.uuid(), notNullValue());
    assertThat(result.sentence(), equalTo("Hello"));
    assertThat(result.translation(), equalTo("new translation"));
    assertThat(result.description(), equalTo("new description"));
  }

  @Test
  void updateANotExistingWord() {
    UUID randomUUID = UUID.randomUUID();
    Word updateWord = new Word(null, null, "new translation", "new description", null, null);
    DataNotFoundException ex = assertThrows(DataNotFoundException.class,
        () -> wordRepository.update(randomUUID, updateWord, USER_ENRICO_ID));
    assertThat(ex.getMessage(), equalTo("Word not found: " + randomUUID));
  }


}
