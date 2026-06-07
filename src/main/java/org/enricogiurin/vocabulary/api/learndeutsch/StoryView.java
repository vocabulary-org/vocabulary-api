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

import java.util.List;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;

/**
 * A complete German gap-fill story with its gaps and options, as returned by
 * {@code GET /public/deutsch/stories/{id}}. The {@code body} contains {@code {{n}}}
 * markers; each one corresponds to the gap with the matching {@code position}.
 */
public record StoryView(
    UUID uuid,
    String title,
    DeutschLevel level,
    String topic,
    String body,
    List<StoryGapView> gaps) {

}
