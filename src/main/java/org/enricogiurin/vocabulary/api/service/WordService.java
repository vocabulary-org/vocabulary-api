package org.enricogiurin.vocabulary.api.service;

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

import com.yourrents.services.common.searchable.Searchable;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.model.Word;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.enricogiurin.vocabulary.api.repository.WordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WordService {

  private final UserRepository userRepository;
  private final WordRepository wordRepository;

  public Word createNewWord(Word word, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    return wordRepository.create(word, userId);
  }

  public Word updateAnExistingWord(UUID uuid, Word word, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    return wordRepository.update(uuid, word, userId);
  }

  public void deleteAnExistingWord(UUID uuid, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    wordRepository.delete(uuid, userId);
  }

  public Optional<Word> findByExternalId(UUID externalId, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    return wordRepository.findByExternalId(externalId, userId);
  }

  public Page<Word> find(Searchable filter, Pageable pageable, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    return wordRepository.find(filter, pageable, userId);
  }


}
