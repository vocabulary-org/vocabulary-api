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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.anthropic.AnthropicTranslationGenerator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NounDeTranslationService {

  private final AnthropicTranslationGenerator generator;
  private final PublicNounDeRepository repository;

  @Async("asyncExecutor")
  public void generateMissingTranslationsAsync(String lang, Integer limit) {
    generateMissingTranslations(lang, limit);
  }

  public GenerationSummary generateMissingTranslations(String lang, Integer limit) {
    List<NounForTranslation> nouns = repository.findWithMissingTranslation(lang, limit);
    log.info("Found {} nouns with missing translation for lang={}", nouns.size(), lang);

    int generated = 0;
    int failed = 0;

    for (NounForTranslation noun : nouns) {
      try {
        String translation = generator.generate(noun.article().getLiteral(), noun.wordDe(), lang);
        repository.saveTranslation(noun.nounId(), lang, translation);
        generated++;
        log.debug("Generated translation for '{} {}' -> {}: {}",
            noun.article().getLiteral(), noun.wordDe(), lang, translation);
      } catch (Exception e) {
        failed++;
        log.error("Failed to generate translation for '{} {}': {}",
            noun.article().getLiteral(), noun.wordDe(), e.getMessage());
      }
    }

    log.info("Translation generation complete: generated={}, failed={}", generated, failed);
    return new GenerationSummary(generated, failed);
  }
}
