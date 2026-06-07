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
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GapCategory;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GrammaticalCase;

/**
 * A single missing word in a story. {@code position} matches the {@code {{position}}}
 * marker in the story body; {@code grammaticalCase} may be null for categories where it
 * does not apply (e.g. VERB_FORM).
 */
public record StoryGapView(
    int position,
    GapCategory category,
    GrammaticalCase grammaticalCase,
    List<StoryGapOptionView> options) {

}
