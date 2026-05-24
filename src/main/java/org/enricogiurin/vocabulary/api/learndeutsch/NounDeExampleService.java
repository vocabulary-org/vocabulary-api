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
import org.enricogiurin.vocabulary.api.anthropic.AnthropicExampleGenerator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NounDeExampleService {

  private final AnthropicExampleGenerator generator;
  private final PublicNounDeRepository repository;

  @Async("asyncExecutor")
  public void generateMissingExamplesAsync(String lang, Integer limit) {
    generateMissingExamples(lang, limit);
  }

  public GenerationSummary generateMissingExamples(String lang, Integer limit) {
    List<NounForGeneration> nouns = repository.findWithMissingExamples(lang, limit);
    log.info("Found {} nouns with missing examples for lang={}", nouns.size(), lang);

    int generated = 0;
    int failed = 0;

    for (NounForGeneration noun : nouns) {
      try {
        List<ExampleView> examples = generator.generate(
            noun.article().getLiteral(),
            noun.wordDe(),
            lang,
            noun.translation());

        for (ExampleView example : examples) {
          repository.saveExample(noun.translationId(), example.sentenceDe(), example.sentenceTranslation());
        }
        generated++;
        log.debug("Generated {} examples for '{} {}'", examples.size(), noun.article().getLiteral(), noun.wordDe());
      } catch (Exception e) {
        failed++;
        log.error("Failed to generate examples for '{} {}': {}", noun.article().getLiteral(), noun.wordDe(), e.getMessage());
      }
    }

    log.info("Example generation complete: generated={}, failed={}", generated, failed);
    return new GenerationSummary(generated, failed);
  }
}
