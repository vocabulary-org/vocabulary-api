package org.enricogiurin.vocabulary.api.model;

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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.enricogiurin.vocabulary.api.validation.ValidationGroups;

public record Word(UUID uuid,
                   @NotNull(message = SENTENCE_NOT_NULL_CONSTRAINT, groups = ValidationGroups.Post.class)
                   @Size(min = 1, max = 200, message = SENTENCE_CONSTRAINT, groups = {
                       ValidationGroups.Post.class, ValidationGroups.Patch.class})
                   String sentence,

                   @NotNull(message = TRANSLATION_NOT_NULL_CONSTRAINT, groups = ValidationGroups.Post.class)
                   @Size(min = 1, max = 200, message = TRANSLATION_CONSTRAINT, groups = {
                       ValidationGroups.Post.class, ValidationGroups.Patch.class})
                   String translation,

                   @Size(max = 500, message = DESCRIPTION_CONSTRAINT, groups = {
                       ValidationGroups.Post.class, ValidationGroups.Patch.class})
                   String description,

                   @NotNull(message = LANGUAGE_NOT_NULL_CONSTRAINT, groups = ValidationGroups.Post.class)
                   Language language,

                   @NotNull(message = LANGUAGE_TO_NOT_NULL_CONSTRAINT, groups = ValidationGroups.Post.class)
                   Language languageTo) {

  public static final String SENTENCE_NOT_NULL_CONSTRAINT = "sentence must not be null";
  public static final String SENTENCE_CONSTRAINT = "sentence must be between 1 and 200 characters";

  public static final String TRANSLATION_NOT_NULL_CONSTRAINT = "translation must not be null";
  public static final String TRANSLATION_CONSTRAINT = "translation must be between 1 and 200 characters";

  public static final String DESCRIPTION_CONSTRAINT = "description can be up to 500 characters";

  public static final String LANGUAGE_NOT_NULL_CONSTRAINT = "language must not be null";
  public static final String LANGUAGE_TO_NOT_NULL_CONSTRAINT = "languageTo must not be null";
}