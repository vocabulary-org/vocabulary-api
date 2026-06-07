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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;

/**
 * A story produced by the generator before it is persisted. Reuses {@link StoryGapView}
 * and {@link StoryGapOptionView} for the gaps and options, so the generator output maps
 * directly onto the same shape served to clients (minus the persisted uuid/topic).
 *
 * <p>{@code level} is only populated when the generator detects it (e.g. generating a
 * story from pasted source text); it is null for the parameter-based flow, where the
 * level is supplied by the caller.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedStory(
    String title,
    String body,
    List<StoryGapView> gaps,
    DeutschLevel level) {

  /** Convenience constructor for flows where the level is supplied separately. */
  public GeneratedStory(String title, String body, List<StoryGapView> gaps) {
    this(title, body, gaps, null);
  }
}
