package org.enricogiurin.vocabulary.api.learndeutsch;

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

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.anthropic.AnthropicStoryGenerator;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;
import org.springframework.stereotype.Service;

/**
 * Generates a German story via the Anthropic API, validates it, persists it, and returns
 * the saved story. Score is computed in the UI, so options keep their {@code correct} flag.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoryDeService {

  private static final Pattern GAP_MARKER = Pattern.compile("\\{\\{(\\d+)}}");

  private final AnthropicStoryGenerator generator;
  private final PublicStoryDeRepository repository;

  public StoryView generateAndSave(DeutschLevel level, String topic, StoryLength length) {
    log.info("Generating DE story: level={}, topic={}, length={}", level, topic, length);
    GeneratedStory generated = generator.generate(level, topic, length);
    validate(generated);
    return persistAndReload(generated, level, topic);
  }

  /**
   * Builds and saves a story from a pasted German text. The level is taken from the request
   * when provided, otherwise the level detected by the generator. Title and topic fall back
   * to the generated title and "custom" respectively when not supplied.
   */
  public StoryView generateFromText(String text, String title, String topic, DeutschLevel requestedLevel) {
    log.info("Generating DE story from text: requestedLevel={}, topic={}, {} chars",
        requestedLevel, topic, text == null ? 0 : text.length());
    GeneratedStory generated = generator.generateFromText(text, requestedLevel);
    validate(generated);

    DeutschLevel level = requestedLevel != null ? requestedLevel : generated.level();
    if (level == null) {
      throw new DataExecutionException("Could not determine the level of the generated story");
    }
    String effectiveTitle = (title != null && !title.isBlank()) ? title : generated.title();
    String effectiveTopic = (topic != null && !topic.isBlank()) ? topic : "custom";

    GeneratedStory toSave = new GeneratedStory(effectiveTitle, generated.body(), generated.gaps(), level);
    return persistAndReload(toSave, level, effectiveTopic);
  }

  /** Deletes a story (with its gaps and options) by external id; returns true if removed. */
  public boolean delete(UUID externalId) {
    log.info("Deleting DE story {}", externalId);
    return repository.delete(externalId);
  }

  private StoryView persistAndReload(GeneratedStory story, DeutschLevel level, String topic) {
    UUID externalId = repository.save(story, level, topic);
    log.info("Saved generated DE story '{}' with id {}", story.title(), externalId);
    return repository.findByExternalId(externalId)
        .orElseThrow(() -> new DataExecutionException("Saved story could not be reloaded: " + externalId));
  }

  /**
   * Rejects a generated story whose markers and gaps are inconsistent, so malformed
   * generations never reach the database.
   */
  void validate(GeneratedStory story) {
    if (story == null || story.body() == null || story.body().isBlank()) {
      throw new DataExecutionException("Generated story has no body");
    }
    if (story.title() == null || story.title().isBlank()) {
      throw new DataExecutionException("Generated story has no title");
    }
    if (story.gaps() == null || story.gaps().isEmpty()) {
      throw new DataExecutionException("Generated story has no gaps");
    }

    Set<Integer> markerPositions = markerPositions(story.body());
    Set<Integer> gapPositions = story.gaps().stream()
        .map(StoryGapView::position)
        .collect(Collectors.toSet());
    if (!markerPositions.equals(gapPositions)) {
      throw new DataExecutionException(
          "Body markers " + markerPositions + " do not match gap positions " + gapPositions);
    }

    for (StoryGapView gap : story.gaps()) {
      if (gap.options() == null || gap.options().isEmpty()) {
        throw new DataExecutionException("Gap " + gap.position() + " has no options");
      }
      long correct = gap.options().stream().filter(StoryGapOptionView::correct).count();
      if (correct != 1) {
        throw new DataExecutionException(
            "Gap " + gap.position() + " must have exactly one correct option but had " + correct);
      }
    }
  }

  private Set<Integer> markerPositions(String body) {
    Matcher matcher = GAP_MARKER.matcher(body);
    Set<Integer> positions = new java.util.HashSet<>();
    while (matcher.find()) {
      positions.add(Integer.valueOf(matcher.group(1)));
    }
    return positions;
  }
}
