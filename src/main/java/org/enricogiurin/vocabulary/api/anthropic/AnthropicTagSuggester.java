package org.enricogiurin.vocabulary.api.anthropic;

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
import org.enricogiurin.vocabulary.api.model.Tag;
import org.enricogiurin.vocabulary.api.model.TagSuggestion;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnthropicTagSuggester {

  private static final String MESSAGES_PATH = "/v1/messages";

  private final RestClient anthropicRestClient;
  private final AnthropicProperties properties;

  public List<TagSuggestion> suggestTags(String sentence, String languageCode,
      List<Tag> availableTags) {
    String tagList = availableTags.stream()
        .map(t -> "- %s: %s".formatted(t.name(), t.description()))
        .collect(java.util.stream.Collectors.joining("\n"));
    String prompt = """
        You are a tagging assistant for a vocabulary learning app.

        Given these predefined tags:
        %s

        Analyze the following sentence and return a JSON array of the applicable tags (maximum 3).
        For each tag include:
        - "tag": the English key exactly as listed above
        - "label": the tag name translated into the language with code "%s"

        Return ONLY the JSON array with no explanation or markdown.

        Sentence: "%s"
        """.formatted(tagList, languageCode, sentence);

    AnthropicRequest request = new AnthropicRequest(
        properties.model(),
        300,
        List.of(new AnthropicMessage("user", prompt))
    );

    AnthropicResponse response = anthropicRestClient.post()
        .uri(MESSAGES_PATH)
        .body(request)
        .retrieve()
        .body(AnthropicResponse.class);

    String json = response.content().getFirst().text();
    log.info("Claude tag suggestion response: {}", json);
    return parseTagSuggestions(json);
  }

  private List<TagSuggestion> parseTagSuggestions(String json) {
    // Parse JSON array like: [{"tag":"TRAVEL","label":"Viaggio"},...]
    json = json.trim();
    if (json.startsWith("[")) {
      json = json.substring(1, json.length() - 1).trim();
    }
    if (json.isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(json.split("\\},\\s*\\{"))
        .map(item -> {
          item = item.replace("{", "").replace("}", "").trim();
          String tag = extractJsonValue(item, "tag");
          String label = extractJsonValue(item, "label");
          return new TagSuggestion(tag, label);
        })
        .filter(s -> s.tag() != null && s.label() != null)
        .toList();
  }

  private String extractJsonValue(String item, String key) {
    String search = "\"" + key + "\"";
    int idx = item.indexOf(search);
    if (idx == -1) {
      return null;
    }
    int colon = item.indexOf(":", idx);
    int start = item.indexOf("\"", colon) + 1;
    int end = item.indexOf("\"", start);
    if (start <= 0 || end <= start) {
      return null;
    }
    return item.substring(start, end);
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
