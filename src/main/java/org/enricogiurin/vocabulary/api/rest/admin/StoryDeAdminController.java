package org.enricogiurin.vocabulary.api.rest.admin;

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

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryDeService;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryFromTextRequest;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryGenerationRequest;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${application.api.admin-path}/deutsch/stories")
@RequiredArgsConstructor
@Slf4j
public class StoryDeAdminController {

  private final StoryDeService storyDeService;

  @PostMapping("/generate")
  public ResponseEntity<StoryView> generate(@Valid @RequestBody StoryGenerationRequest request) {
    log.info("POST /deutsch/stories/generate level={}, topic={}, length={}",
        request.level(), request.topic(), request.length());
    StoryView story = storyDeService.generateAndSave(request.level(), request.topic(), request.length());
    return ResponseEntity.status(HttpStatus.CREATED).body(story);
  }

  @PostMapping("/from-text")
  public ResponseEntity<StoryView> generateFromText(@Valid @RequestBody StoryFromTextRequest request) {
    log.info("POST /deutsch/stories/from-text level={}, topic={}, {} chars",
        request.level(), request.topic(), request.text() == null ? 0 : request.text().length());
    StoryView story = storyDeService.generateFromText(
        request.text(), request.title(), request.topic(), request.level());
    return ResponseEntity.status(HttpStatus.CREATED).body(story);
  }

  @DeleteMapping("/{uuid}")
  public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
    log.info("DELETE /deutsch/stories/{}", uuid);
    return storyDeService.delete(uuid)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }
}
