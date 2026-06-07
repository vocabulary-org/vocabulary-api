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

/**
 * Requested length of a generated story. Drives the approximate number of sentences and
 * gaps requested from the generator; it is not persisted.
 */
public enum StoryLength {

  SHORT(3, "3 to 4 sentences"),
  MEDIUM(5, "6 to 8 sentences"),
  LONG(8, "10 to 12 sentences");

  private final int gaps;
  private final String sentences;

  StoryLength(int gaps, String sentences) {
    this.gaps = gaps;
    this.sentences = sentences;
  }

  public int gaps() {
    return gaps;
  }

  public String sentences() {
    return sentences;
  }
}
