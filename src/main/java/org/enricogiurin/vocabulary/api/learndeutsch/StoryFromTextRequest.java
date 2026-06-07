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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;

/**
 * Admin request to build a gap-fill story on top of a pasted German text.
 * Only {@code text} is required: title, topic and level are optional and are derived
 * (or, for the level, detected) by the generator when omitted.
 */
public record StoryFromTextRequest(
    @NotBlank(message = "text must not be blank")
    @Size(max = 5000, message = "text must be at most 5000 characters")
    String text,

    @Size(max = 256, message = "title must be at most 256 characters")
    String title,

    @Size(max = 64, message = "topic must be at most 64 characters")
    String topic,

    DeutschLevel level) {

}
