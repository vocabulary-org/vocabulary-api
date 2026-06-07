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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;
import org.enricogiurin.vocabulary.api.learndeutsch.GeneratedStory;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryLength;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Generates a German fill-in-the-gap story (text with {@code {{n}}} markers, gaps and
 * "alike" options) via the Anthropic API, given a level, topic and length.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnthropicStoryGenerator {

  private static final String MESSAGES_PATH = "/v1/messages";
  private static final int MAX_TOKENS = 3000;

  private final RestClient anthropicRestClient;
  private final AnthropicProperties properties;
  private final ObjectMapper objectMapper;

  public GeneratedStory generate(DeutschLevel level, String topic, StoryLength length) {
    String userMessage = """
        You are a German teacher creating a fill-in-the-gap grammar assessment.
        Create a German story of %s (exactly %d gaps) for CEFR level %s on the topic "%s".
        Write ONLY in German. Do NOT include any translation into any other language.

        Rules for the gaps:
        - Mark each gap in the body with {{1}}, {{2}}, ... numbered sequentially from 1.
        - The gap numbers in the body MUST exactly match the "position" values, with no gaps missing or extra.
        - Each gap tests ONE grammar point. "category" must be one of:
          ARTICLE, ADJECTIVE_ENDING, PRONOUN, PREPOSITION, VERB_FORM.
        - "grammaticalCase" must be one of NOMINATIV, AKKUSATIV, DATIV, GENITIV, or null
          when it does not apply (e.g. for VERB_FORM).
        - Provide EXACTLY 4 options per gap. All 4 options MUST be different inflected forms
          of the SAME word (e.g. der/den/dem/des, or schöne/schönes/schönen/schönem).
          Exactly ONE option is correct in context; the other three must be real German forms
          that are grammatically wrong in this context.

        Return ONLY valid JSON, with no markdown and no explanation, in EXACTLY this shape:
        {"title":"...","body":"... {{1}} ... {{2}} ...","gaps":[
          {"position":1,"category":"ARTICLE","grammaticalCase":"AKKUSATIV","options":[
            {"text":"der","correct":false},{"text":"den","correct":true},
            {"text":"dem","correct":false},{"text":"des","correct":false}]}]}
        """.formatted(length.sentences(), length.gaps(), level.getLiteral(), topic);

    AnthropicRequest request = new AnthropicRequest(
        properties.model(),
        MAX_TOKENS,
        List.of(new AnthropicMessage("user", userMessage))
    );

    AnthropicResponse response = anthropicRestClient.post()
        .uri(MESSAGES_PATH)
        .body(request)
        .retrieve()
        .body(AnthropicResponse.class);

    assert response != null;
    String json = response.content().getFirst().text();
    log.debug("Claude story response for level={}, topic={}: {}", level, topic, json);
    return parseStory(json);
  }

  GeneratedStory parseStory(String json) {
    try {
      String cleaned = json.strip();
      if (cleaned.startsWith("```")) {
        cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```$", "").strip();
      }
      return objectMapper.readValue(cleaned, GeneratedStory.class);
    } catch (Exception e) {
      log.error("Failed to parse generated story from JSON: {}", json, e);
      throw new DataExecutionException("Could not parse generated story", e);
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
