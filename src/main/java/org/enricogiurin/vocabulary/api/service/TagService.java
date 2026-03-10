package org.enricogiurin.vocabulary.api.service;

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

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.anthropic.AnthropicTagSuggester;
import org.enricogiurin.vocabulary.api.model.Tag;
import org.enricogiurin.vocabulary.api.model.TagSuggestion;
import org.enricogiurin.vocabulary.api.repository.TagRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {

  private final AnthropicTagSuggester anthropicTagSuggester;
  private final TagRepository tagRepository;

  public List<TagSuggestion> suggestTags(String sentence, String languageCode) {
    List<Tag> availableTags = tagRepository.findAll();
    log.info("Suggesting tags for sentence: '{}' language: '{}' using {} available tags",
        sentence, languageCode, availableTags.size());
    return anthropicTagSuggester.suggestTags(sentence, languageCode, availableTags);
  }
}
