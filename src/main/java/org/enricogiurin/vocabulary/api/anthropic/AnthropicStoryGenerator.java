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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
  private static final String STORY_TOOL_NAME = "save_story";
  private static final String STORY_TOOL_DESCRIPTION =
      "Save the generated German fill-in-the-gap story with its gaps and answer options.";

  /**
   * JSON Schema for the {@code save_story} tool input, matching {@link GeneratedStory}. Using a
   * tool lets Claude return the story as a structured value that the API serializes into valid
   * JSON, so quotation marks in the (possibly pasted) German text can no longer break parsing.
   */
  private static final String STORY_SCHEMA_JSON = """
      {
        "type": "object",
        "properties": {
          "level": {"type": "string", "enum": ["A1","A2","B1","B2","C1","C2"]},
          "title": {"type": "string"},
          "body": {"type": "string"},
          "gaps": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "position": {"type": "integer"},
                "category": {"type": "string",
                  "enum": ["ARTICLE","ADJECTIVE_ENDING","PRONOUN","PREPOSITION","VERB_FORM"]},
                "grammaticalCase": {"type": ["string","null"]},
                "options": {
                  "type": "array",
                  "description": "Exactly 4 options, all different inflected forms of the same word. EXACTLY ONE option must have correct=true; the other three must have correct=false.",
                  "items": {
                    "type": "object",
                    "properties": {
                      "text": {"type": "string", "description": "An inflected form of the word."},
                      "correct": {"type": "boolean",
                        "description": "true for the single option that is correct in context, false for the other three. Every gap must have exactly one option set to true."}
                    },
                    "required": ["text","correct"]
                  }
                }
              },
              "required": ["position","category","options"]
            }
          }
        },
        "required": ["title","body","gaps"]
      }
      """;

  private final RestClient anthropicRestClient;
  private final AnthropicProperties properties;
  private final ObjectMapper objectMapper;
  private JsonNode storySchema;

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

        Return the result by calling the `save_story` tool: put the German story (with the
        {{n}} markers) in "body" and the gaps in "gaps". Each gap's "options" must contain
        EXACTLY 4 entries, and EXACTLY ONE of them must have correct=true, e.g.
        [{"text":"der","correct":false},{"text":"den","correct":true},
         {"text":"dem","correct":false},{"text":"des","correct":false}].
        """.formatted(length.sentences(), length.gaps(), level.getLiteral(), topic);

    return call(userMessage, "level=" + level + ", topic=" + topic);
  }

  /**
   * Builds a gap-fill story on top of an existing German text, keeping its wording and only
   * blanking out words. When {@code requestedLevel} is null the generator detects the CEFR
   * level and returns it in {@link GeneratedStory#level()}.
   */
  public GeneratedStory generateFromText(String text, DeutschLevel requestedLevel) {
    String levelInstruction = requestedLevel != null
        ? "The target CEFR level is %s; return it as \"level\".".formatted(requestedLevel.getLiteral())
        : "Detect the CEFR level (A1, A2, B1, B2, C1 or C2) of the text and return it as \"level\".";
    String userMessage = """
        You are a German teacher creating a fill-in-the-gap grammar assessment from an existing text.
        Use the SOURCE TEXT at the end. The SOURCE TEXT is the ground truth and is always correct:
        do NOT rewrite, correct, rephrase, or change its wording, spelling, punctuation or
        capitalization. Only blank out some existing words. Write ONLY in German. Do NOT translate.
        %s

        Rules for the gaps:
        - Choose between 3 and 10 words that already appear in the text, picking words that test
          grammar: articles, adjective endings, pronouns, prepositions, or verb forms.
        - Replace each chosen word in the body with {{1}}, {{2}}, ... numbered sequentially from 1,
          leaving the rest of the text byte-for-byte unchanged.
        - The gap numbers in the body MUST exactly match the "position" values, none missing or extra.
        - Each gap tests ONE grammar point. "category" must be one of:
          ARTICLE, ADJECTIVE_ENDING, PRONOUN, PREPOSITION, VERB_FORM.
        - "grammaticalCase" must be one of NOMINATIV, AKKUSATIV, DATIV, GENITIV, or null
          when it does not apply (e.g. for VERB_FORM).
        - Provide EXACTLY 4 options per gap, all different inflected forms of the SAME word
          (e.g. der/den/dem/die). The CORRECT option (correct=true) MUST be the EXACT word that
          was originally in the text at that spot — copy it verbatim. The other three (correct=false)
          are "alike" inflected forms that are grammatically wrong in this context.
        - Invariant: if every gap {{n}} were filled back in with its correct=true option, the
          resulting text MUST be identical to the SOURCE TEXT. Verify this before answering.

        For example, if the text contains "... ist der Fonds ..." and you blank out "der", then
        "der" is the correct option and the alikes are den/dem/die:
        [{"text":"der","correct":true},{"text":"den","correct":false},
         {"text":"dem","correct":false},{"text":"die","correct":false}].

        Return the result by calling the `save_story` tool. Put the gapped text in "body" (keep all
        original wording and quotation marks) and the gaps in "gaps".

        SOURCE TEXT:
        %s
        """.formatted(levelInstruction, text);

    return call(userMessage, "fromText, requestedLevel=" + requestedLevel);
  }

  private GeneratedStory call(String userMessage, String logContext) {
    AnthropicRequest request = new AnthropicRequest(
        properties.model(),
        MAX_TOKENS,
        List.of(new AnthropicMessage("user", userMessage)),
        List.of(new Tool(STORY_TOOL_NAME, STORY_TOOL_DESCRIPTION, storySchema())),
        new ToolChoice("tool", STORY_TOOL_NAME)
    );

    AnthropicResponse response = anthropicRestClient.post()
        .uri(MESSAGES_PATH)
        .body(request)
        .retrieve()
        .body(AnthropicResponse.class);

    assert response != null;
    JsonNode input = extractToolInput(response);
    log.debug("Claude story tool input ({}): {}", logContext, input);
    return parseStory(input);
  }

  GeneratedStory parseStory(JsonNode toolInput) {
    try {
      return objectMapper.treeToValue(toolInput, GeneratedStory.class);
    } catch (Exception e) {
      log.error("Failed to parse generated story from tool input: {}", toolInput, e);
      throw new DataExecutionException("Could not parse generated story", e);
    }
  }

  /**
   * Returns the {@code input} of the first {@code tool_use} content block. Because {@code
   * tool_choice} forces the {@code save_story} tool, Claude returns the story as a structured
   * object that the API serializes into valid JSON, rather than as free-form JSON text.
   */
  private static JsonNode extractToolInput(AnthropicResponse response) {
    return response.content().stream()
        .map(AnthropicContent::input)
        .filter(input -> input != null && !input.isNull())
        .findFirst()
        .orElseThrow(() -> new DataExecutionException("No tool_use block in Claude response"));
  }

  private JsonNode storySchema() {
    if (storySchema == null) {
      try {
        storySchema = objectMapper.readTree(STORY_SCHEMA_JSON);
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Invalid story tool schema", e);
      }
    }
    return storySchema;
  }

  record AnthropicRequest(String model, int max_tokens, List<AnthropicMessage> messages,
                          List<Tool> tools, ToolChoice tool_choice) {
  }

  record AnthropicMessage(String role, String content) {
  }

  record Tool(String name, String description, JsonNode input_schema) {
  }

  record ToolChoice(String type, String name) {
  }

  record AnthropicResponse(List<AnthropicContent> content) {
  }

  record AnthropicContent(String type, String text, JsonNode input) {
  }
}
