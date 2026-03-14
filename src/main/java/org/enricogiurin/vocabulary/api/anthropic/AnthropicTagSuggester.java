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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.model.TagSuggestion;
import org.enricogiurin.vocabulary.api.repository.TagRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnthropicTagSuggester {

  private static final String MESSAGES_PATH = "/v1/messages";

  private final RestClient anthropicRestClient;
  private final AnthropicProperties properties;
  private final ObjectMapper objectMapper;
  private final TagRepository tagRepository;

  @Getter(AccessLevel.PACKAGE)
  private String systemPrompt;

  @PostConstruct
  void init() {
    String tagList = tagRepository.findAll().stream()
        .map(t -> "- %s: %s".formatted(t.name(), t.description()))
        .collect(Collectors.joining("\n"));
    systemPrompt = """
        You are a tagging assistant for a vocabulary learning app.

        Given these predefined tags:
        %s

        When asked, analyze the given sentence and return a JSON array of the applicable tags (maximum 3).
        For each tag include:
        - "tag": the English key exactly as listed above
        - "label": the tag name translated into the requested language
        """.formatted(tagList);
  }

  public List<TagSuggestion> suggestTags(String sentence, String languageCode) {
    String userMessage = """
        Analyze this sentence and return a JSON array of applicable tags (maximum 3).
        Translate the "label" field into language code "%s".
        Return ONLY the JSON array with no explanation or markdown.

        Sentence: "%s"
        """.formatted(languageCode, sentence);

    AnthropicRequest request = new AnthropicRequest(
        properties.model(),
        300,
        List.of(new AnthropicSystemContent("text", this.systemPrompt, new CacheControl("ephemeral"))),
        List.of(new AnthropicMessage("user", userMessage))
    );

    AnthropicResponse response = anthropicRestClient.post()
        .uri(MESSAGES_PATH)
        .body(request)
        .retrieve()
        .body(AnthropicResponse.class);

    assert response != null;
    String json = response.content().getFirst().text();
    log.info("Claude tag suggestion response: {}", json);
    return parseTagSuggestions(json);
  }

  List<TagSuggestion> parseTagSuggestions(String json) {
    try {
      String cleaned = json.strip();
      if (cleaned.startsWith("```")) {
        cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```$", "").strip();
      }
      return objectMapper.readValue(cleaned, new TypeReference<>() {});
    } catch (Exception e) {
      log.error("Failed to parse tag suggestions from JSON: {}", json, e);
      return List.of();
    }
  }

  record AnthropicRequest(String model, int max_tokens, List<AnthropicSystemContent> system,
      List<AnthropicMessage> messages) {
  }

  record AnthropicSystemContent(String type, String text, CacheControl cache_control) {
  }

  record CacheControl(String type) {
  }

  record AnthropicMessage(String role, String content) {
  }

  record AnthropicResponse(List<AnthropicContent> content) {
  }

  record AnthropicContent(String type, String text) {
  }
}
