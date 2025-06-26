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

import org.jooq.impl.EnumConverter;

public class LanguageConverter extends
    EnumConverter<org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.Language, Language> {

  public LanguageConverter() {
    super(org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.Language.class, Language.class);
  }

  @Override
  public org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.Language to(Language language) {
    return org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.Language.valueOf(
        language.getLanguage());
  }
}
