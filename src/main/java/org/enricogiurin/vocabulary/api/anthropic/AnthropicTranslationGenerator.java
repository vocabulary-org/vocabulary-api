package org.enricogiurin.vocabulary.api.anthropic;

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
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnthropicTranslationGenerator {

  private static final String MESSAGES_PATH = "/v1/messages";
  private static final int MAX_TOKENS = 100;

  private static final Map<String, String> LANG_NAMES = Map.of(
      "it", "Italian",
      "en", "English",
      "es", "Spanish"
  );

  private final RestClient anthropicRestClient;
  private final AnthropicProperties properties;

  public String generate(String article, String wordDe, String lang) {
    String langName = LANG_NAMES.getOrDefault(lang, lang);
    String userMessage = """
        Translate the German noun "%s %s" into %s.
        Return ONLY the translated word or short phrase, no article, no explanation.
        """.formatted(article, wordDe, langName);

    AnthropicRequest request = new AnthropicRequest(
        properties.model(),
        MAX_TOKENS,
        List.of(new AnthropicMessage("user", userMessage)),
        AnthropicThinking.DISABLED
    );

    AnthropicResponse response = anthropicRestClient.post()
        .uri(MESSAGES_PATH)
        .body(request)
        .retrieve()
        .body(AnthropicResponse.class);

    String translation = firstText(response)
        .orElseThrow(() -> new DataExecutionException(
            "No text in Claude response for '%s %s' -> %s".formatted(article, wordDe, lang)))
        .strip();
    log.debug("Claude translation for '{} {}' -> {}: {}", article, wordDe, lang, translation);
    return translation;
  }

  private static Optional<String> firstText(AnthropicResponse response) {
    if (response == null || response.content() == null) {
      return Optional.empty();
    }
    return response.content().stream()
        .filter(c -> "text".equals(c.type()))
        .map(AnthropicContent::text)
        .findFirst();
  }

  record AnthropicRequest(String model, int max_tokens, List<AnthropicMessage> messages,
                          AnthropicThinking thinking) {
  }

  record AnthropicMessage(String role, String content) {
  }

  record AnthropicResponse(List<AnthropicContent> content) {
  }

  record AnthropicContent(String type, String text) {
  }
}
