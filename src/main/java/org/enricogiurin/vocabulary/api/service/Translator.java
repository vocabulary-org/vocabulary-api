package org.enricogiurin.vocabulary.api.service;

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

import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.enricogiurin.vocabulary.api.model.TranslateResponse;

/**
 * Translates a text into one or more target languages.
 *
 * <p>The active implementation is selected with {@code application.translation.provider}
 * ({@code claude} by default, or {@code azure}). Implementations only call the provider: quota
 * handling lives in {@link TranslateService}.
 */
public interface Translator {

  /**
   * @return one translation per requested target language
   * @throws org.enricogiurin.vocabulary.api.exception.TranslationFailedException if the provider
   *     call fails or returns an unusable result
   */
  TranslateResponse translate(TranslateRequest request);
}
