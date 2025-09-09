package org.enricogiurin.vocabulary.api.rest.me;

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
import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.model.Word;
import org.enricogiurin.vocabulary.api.repository.WordRepository;
import org.enricogiurin.vocabulary.api.security.PrincipalAccessor;
import org.enricogiurin.vocabulary.api.service.WordService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${application.api.user-path}/words")
@RequiredArgsConstructor
public class WordController {

  private final WordService wordService;
  private final PrincipalAccessor principalAccessor;

  @GetMapping
  ResponseEntity<Page<Word>> find(
      @ParameterObject Searchable filter,
      @ParameterObject @SortDefault(sort = WordRepository.SENTENCE_ALIAS, direction = Direction.ASC) Pageable pagination) {
    String keycloakId = principalAccessor.getSubject();
    Page<Word> page = wordService.find(filter, pagination, keycloakId);
    return ResponseEntity.ok(page);
  }

  @GetMapping("/{uuid}")
  ResponseEntity<Word> findByUuid(@PathVariable UUID uuid) {
    String subject = principalAccessor.getSubject();
    Word result = wordService.findByExternalId(uuid, subject)
        .orElseThrow(
            () -> new DataNotFoundException("can't find Word having uuid: " + uuid));
    return ResponseEntity.ok(result);
  }

  @PostMapping
  ResponseEntity<Word> add(@RequestBody Word word) {
    String subject = principalAccessor.getSubject();
    Word savedProperty = wordService.createNewWord(word, subject);
    return new ResponseEntity<>(savedProperty, HttpStatus.CREATED);
  }

  @PatchMapping("/{uuid}")
  ResponseEntity<Word> update(@PathVariable UUID uuid, @RequestBody Word wordToUpdate) {
    String subject = principalAccessor.getSubject();
    Word updatedProperty = wordService.updateAnExistingWord(uuid, wordToUpdate, subject);
    return ResponseEntity.ok(updatedProperty);
  }

  @DeleteMapping("/{uuid}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID uuid) {
    String subject = principalAccessor.getSubject();
    wordService.deleteAnExistingWord(uuid, subject);
  }

}
