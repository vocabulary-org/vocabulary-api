package org.enricogiurin.vocabulary.api.rest.pub;

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

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.learndeutsch.PublicStoryDeRepository;
import org.enricogiurin.vocabulary.api.learndeutsch.StorySummaryView;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${application.api.public-path}/deutsch/stories")
@RequiredArgsConstructor
@Slf4j
public class StoryDeController {

  private final PublicStoryDeRepository publicStoryDeRepository;

  @GetMapping
  public ResponseEntity<List<StorySummaryView>> getStories() {
    log.info("Request received to retrieve the list of DE stories.");
    return ResponseEntity.ok(publicStoryDeRepository.findAll());
  }

  @GetMapping("/{uuid}")
  public ResponseEntity<StoryView> getStory(@PathVariable UUID uuid) {
    log.info("Request received to retrieve DE story {}.", uuid);
    return publicStoryDeRepository.findByExternalId(uuid)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
