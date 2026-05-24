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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.learndeutsch.ExampleView;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnthropicExampleGenerator {

  private static final String MESSAGES_PATH = "/v1/messages";
  private static final int NUM_EXAMPLES = 2;

  private static final Map<String, String> LANG_NAMES = Map.of(
      "it", "Italian",
      "en", "English",
      "es", "Spanish"
  );

  private final RestClient anthropicRestClient;
  private final AnthropicProperties properties;
  private final ObjectMapper objectMapper;

  public List<ExampleView> generate(String article, String wordDe, String lang, String translation) {
    String langName = LANG_NAMES.getOrDefault(lang, lang);
    String userMessage = """
        Generate %d simple example sentences in German using the noun "%s %s" (which means "%s" in %s).
        Each sentence should be natural and suitable for beginner language learners.
        For each sentence provide the German text and its %s translation.
        Return ONLY a JSON array with no markdown or explanation:
        [{"sentenceDe": "...", "sentenceTranslation": "..."}]
        """.formatted(NUM_EXAMPLES, article, wordDe, translation, langName, langName);

    AnthropicRequest request = new AnthropicRequest(
        properties.model(),
        500,
        List.of(new AnthropicMessage("user", userMessage))
    );

    AnthropicResponse response = anthropicRestClient.post()
        .uri(MESSAGES_PATH)
        .body(request)
        .retrieve()
        .body(AnthropicResponse.class);

    assert response != null;
    String json = response.content().getFirst().text();
    log.debug("Claude example response for '{} {}': {}", article, wordDe, json);
    return parseExamples(json);
  }

  List<ExampleView> parseExamples(String json) {
    try {
      String cleaned = json.strip();
      if (cleaned.startsWith("```")) {
        cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```$", "").strip();
      }
      return objectMapper.readValue(cleaned, new TypeReference<>() {});
    } catch (Exception e) {
      log.error("Failed to parse example sentences from JSON: {}", json, e);
      return List.of();
    }
  }

  record AnthropicRequest(String model, int max_tokens, List<AnthropicMessage> messages) {
  }

  record AnthropicMessage(String role, String content) {
  }

  record AnthropicResponse(List<AnthropicContent> content) {
  }

  record AnthropicContent(String type, String text) {
  }
}
