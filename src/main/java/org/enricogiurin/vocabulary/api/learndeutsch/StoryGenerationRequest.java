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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;

/**
 * Admin request to generate a new German story: CEFR level, topic and desired length.
 */
public record StoryGenerationRequest(
    @NotNull(message = "level must not be null")
    DeutschLevel level,

    @NotBlank(message = "topic must not be blank")
    @Size(max = 64, message = "topic must be at most 64 characters")
    String topic,

    @NotNull(message = "length must not be null")
    StoryLength length) {

}
