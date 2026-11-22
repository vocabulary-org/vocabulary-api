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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.TranslationFailedException;
import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.enricogiurin.vocabulary.api.model.TranslateResponse;
import org.enricogiurin.vocabulary.api.service.Translator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link Translator} backed by the Claude Messages API. This is the default provider
 * ({@code application.translation.provider=claude}).
 *
 * <p>All target languages are translated in a single call. The answer is constrained with a JSON
 * schema ({@code output_config.format}), so it can always be parsed.
 */
@Component
@ConditionalOnProperty(name = "application.translation.provider", havingValue = "claude",
    matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ClaudeTranslator implements Translator {

  static final String MESSAGES_PATH = "/v1/messages";
  /** Enough for 5 target languages with up to 3 meanings each; the output is a few dozen tokens. */
  static final int MAX_TOKENS = 512;

  static final String SYSTEM_PROMPT = """
      You are a translation engine for a vocabulary learning app.
      Translate the user's text from the source language into each requested target language.
      Languages are given as ISO 639-1 codes.
      - If the text is a single word or a short expression with several common meanings
        (e.g. German "Bank", "Schloss", "Gericht", "aufheben"), return the most common meaning
        first, followed by at most two other common meanings, separated by ", ".
      - Otherwise return exactly one natural translation.
      - Keep the grammatical form of the source (a noun stays a noun, a verb stays a verb).
      - Do not add explanations, quotes, or articles that are not in the source.
      Return one entry per target language, using the target code exactly as given.
      """;

  private static final String TRANSLATION_SCHEMA_JSON = """
      {
        "type": "object",
        "properties": {
          "translations": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "to": {"type": "string"},
                "text": {"type": "string"}
              },
              "required": ["to", "text"],
              "additionalProperties": false
            }
          }
        },
        "required": ["translations"],
        "additionalProperties": false
      }
      """;

  private final RestClient anthropicRestClient;
  private final AnthropicProperties properties;
  private final ObjectMapper objectMapper;

  private JsonNode translationSchema;

  @Override
  public TranslateResponse translate(TranslateRequest request) {
    String userMessage = """
        Source language: %s
        Target languages: %s
        Text: %s
        """.formatted(request.from(), String.join(", ", request.to()), request.text());

    AnthropicRequest body = new AnthropicRequest(
        properties.model(),
        MAX_TOKENS,
        SYSTEM_PROMPT,
        List.of(new AnthropicMessage("user", userMessage)),
        AnthropicThinking.DISABLED,
        new OutputConfig(new OutputFormat("json_schema", translationSchema()))
    );

    AnthropicResponse response = call(body);
    AnthropicUsage usage = response.usage();
    log.info("Claude translation {}->{}: id={}, model={}, input_tokens={}, output_tokens={}, "
            + "stop_reason={}",
        request.from(), request.to(), response.id(), response.model(),
        usage == null ? null : usage.input_tokens(),
        usage == null ? null : usage.output_tokens(),
        response.stop_reason());
    TranslateResponse result = toTranslateResponse(request, response);
    log.debug("Claude translation {} -> {}", request, result);
    return result;
  }

  private AnthropicResponse call(AnthropicRequest body) {
    try {
      AnthropicResponse response = anthropicRestClient.post()
          .uri(MESSAGES_PATH)
          .body(body)
          .retrieve()
          .body(AnthropicResponse.class);
      if (response == null) {
        throw new TranslationFailedException("Claude translation failed: empty response", null);
      }
      return response;
    } catch (RestClientResponseException ex) {
      throw new TranslationFailedException(
          "Claude translation call failed: " + ex.getStatusCode() + " - "
              + ex.getResponseBodyAsString(),
          ex);
    } catch (RestClientException ex) {
      throw new TranslationFailedException("Claude translation call failed: " + ex.getMessage(),
          ex);
    }
  }

  /**
   * Maps Claude's JSON answer to a {@link TranslateResponse}, in the order of the requested target
   * languages. Fails if the answer is incomplete (refusal, truncation, or a missing language).
   */
  private TranslateResponse toTranslateResponse(TranslateRequest request,
      AnthropicResponse response) {
    if (!"end_turn".equals(response.stop_reason())) {
      throw new TranslationFailedException(
          "Claude translation did not complete, stop_reason=" + response.stop_reason(), null);
    }
    String json = response.content() == null ? null : response.content().stream()
        .filter(c -> "text".equals(c.type()))
        .map(AnthropicContent::text)
        .findFirst()
        .orElse(null);
    if (json == null) {
      throw new TranslationFailedException("Claude translation returned no text", null);
    }

    ClaudeTranslations parsed;
    try {
      parsed = objectMapper.readValue(json, ClaudeTranslations.class);
    } catch (JacksonException ex) {
      throw new TranslationFailedException("Could not parse Claude translation: " + json, ex);
    }

    Map<String, String> byLanguage = new LinkedHashMap<>();
    parsed.translations().forEach(t -> byLanguage.putIfAbsent(t.to(), t.text().strip()));

    List<TranslateResponse.Translation> translations = request.to().stream()
        .map(lang -> {
          String text = byLanguage.get(lang);
          if (text == null || text.isBlank()) {
            throw new TranslationFailedException(
                "Claude translation is missing target language '" + lang + "': " + json, null);
          }
          return new TranslateResponse.Translation(text, lang);
        })
        .toList();
    return new TranslateResponse(translations);
  }

  private JsonNode translationSchema() {
    if (translationSchema == null) {
      try {
        translationSchema = objectMapper.readTree(TRANSLATION_SCHEMA_JSON);
      } catch (JacksonException e) {
        throw new IllegalStateException("Invalid translation schema", e);
      }
    }
    return translationSchema;
  }

  record AnthropicRequest(String model, int max_tokens, String system,
                          List<AnthropicMessage> messages, AnthropicThinking thinking,
                          OutputConfig output_config) {
  }

  record AnthropicMessage(String role, String content) {
  }

  record OutputConfig(OutputFormat format) {
  }

  record OutputFormat(String type, JsonNode schema) {
  }

  record AnthropicResponse(String id, String model, List<AnthropicContent> content,
                           String stop_reason, AnthropicUsage usage) {
  }

  record AnthropicUsage(Integer input_tokens, Integer output_tokens) {
  }

  record AnthropicContent(String type, String text) {
  }

  record ClaudeTranslations(List<ClaudeTranslation> translations) {
  }

  record ClaudeTranslation(String to, String text) {
  }
}
