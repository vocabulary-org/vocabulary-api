package org.enricogiurin.vocabulary.api.rest.me;

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

import com.yourrents.services.common.util.exception.DataNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.flashcard.WordLearning;
import org.enricogiurin.vocabulary.api.flashcard.WordLearningRepository;
import org.enricogiurin.vocabulary.api.flashcard.WordReviewResult;
import org.enricogiurin.vocabulary.api.flashcard.WordView;
import org.enricogiurin.vocabulary.api.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${application.api.user-path}/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

  private final WordLearningRepository wordLearningRepository;
  private final CurrentUser currentUser;

  @GetMapping("/words")
  ResponseEntity<List<WordView>> findWordsForReview(
      @RequestParam UUID language,
      @RequestParam UUID languageTo,
      @RequestParam(defaultValue = "10") int limit) {
    List<WordView> words = wordLearningRepository.findWordsForReview(
        language, languageTo, currentUser.getUserId(), limit);
    return ResponseEntity.ok(words);
  }

  @GetMapping("/words/{wordUuid}")
  ResponseEntity<WordLearning> findByWordUuid(@PathVariable UUID wordUuid) {
    WordLearning result = wordLearningRepository.findByWordExternalId(wordUuid)
        .orElseThrow(() -> new DataNotFoundException(
            "No learning record found for word: " + wordUuid));
    return ResponseEntity.ok(result);
  }

  @PostMapping("/review")
  ResponseEntity<WordLearning> review(@RequestBody WordReviewResult wordReviewResult) {
    WordLearning result = wordLearningRepository.review(wordReviewResult);
    return ResponseEntity.ok(result);
  }
}
