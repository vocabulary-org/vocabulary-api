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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.enricogiurin.vocabulary.api.exception.TranslationFailedException;
import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.enricogiurin.vocabulary.api.model.TranslateResponse;
import org.enricogiurin.vocabulary.api.model.TranslateResponse.Translation;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class ClaudeTranslatorTest {

  private static final String MODEL = "claude-haiku-5-5";

  private MockRestServiceServer server;
  private ClaudeTranslator instance;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://mock.anthropic.local");
    server = MockRestServiceServer.bindTo(builder).build();
    instance = new ClaudeTranslator(builder.build(),
        new AnthropicProperties("dummy", MODEL, "https://mock.anthropic.local"),
        JsonMapper.builder().build());
  }

  /** Builds a Messages API response whose single text block is {@code json}. */
  private static String claudeResponse(String json, String stopReason) {
    String escaped = json.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    return """
        {
          "id": "msg_1",
          "type": "message",
          "role": "assistant",
          "model": "%s",
          "content": [{"type": "text", "text": "%s"}],
          "stop_reason": "%s",
          "usage": {"input_tokens": 120, "output_tokens": 30}
        }
        """.formatted(MODEL, escaped, stopReason);
  }

  @Test
  void translate_ok_inRequestedOrder(CapturedOutput output) {
    // given
    TranslateRequest req = new TranslateRequest("Bank", "de", List.of("it", "en"));
    String answer = """
        {"translations": [
          {"to": "en", "text": "bank, bench"},
          {"to": "it", "text": "banca, panchina"}
        ]}""";
    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.model").value(MODEL))
        .andExpect(jsonPath("$.max_tokens").value(ClaudeTranslator.MAX_TOKENS))
        .andExpect(jsonPath("$.thinking.type").value("disabled"))
        .andExpect(jsonPath("$.output_config.format.type").value("json_schema"))
        .andExpect(jsonPath("$.output_config.format.schema.required[0]").value("translations"))
        .andExpect(jsonPath("$.system").value(ClaudeTranslator.SYSTEM_PROMPT))
        .andExpect(jsonPath("$.messages[0].role").value("user"))
        .andExpect(jsonPath("$.messages[0].content").value(Matchers.containsString("Text: Bank")))
        .andExpect(jsonPath("$.messages[0].content")
            .value(Matchers.containsString("Target languages: it, en")))
        .andRespond(withSuccess(claudeResponse(answer, "end_turn"), MediaType.APPLICATION_JSON));

    // when
    TranslateResponse result = instance.translate(req);

    // then
    assertThat(result.translations())
        .extracting(Translation::to, Translation::text)
        .containsExactly(
            tuple("it", "banca, panchina"),
            tuple("en", "bank, bench"));
    assertThat(output).contains(
        "Claude translation de->[it, en]: id=msg_1, model=claude-haiku-5-5, "
            + "input_tokens=120, output_tokens=30, stop_reason=end_turn");
    server.verify();
  }

  @Test
  void translate_missingLanguage_fails() {
    TranslateRequest req = new TranslateRequest("Hund", "de", List.of("it", "es"));
    String answer = """
        {"translations": [{"to": "it", "text": "cane"}]}""";
    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andRespond(withSuccess(claudeResponse(answer, "end_turn"), MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> instance.translate(req))
        .isInstanceOf(TranslationFailedException.class)
        .hasMessageContaining("missing target language 'es'");
  }

  @Test
  void translate_refusal_fails() {
    TranslateRequest req = new TranslateRequest("Hund", "de", List.of("it"));
    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andRespond(withSuccess(claudeResponse("", "refusal"), MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> instance.translate(req))
        .isInstanceOf(TranslationFailedException.class)
        .hasMessageContaining("stop_reason=refusal");
  }

  @Test
  void translate_truncated_fails() {
    TranslateRequest req = new TranslateRequest("Hund", "de", List.of("it"));
    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andRespond(withSuccess(claudeResponse("{\"translations\": [", "max_tokens"),
            MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> instance.translate(req))
        .isInstanceOf(TranslationFailedException.class)
        .hasMessageContaining("stop_reason=max_tokens");
  }

  @Test
  void translate_serverError_fails() {
    TranslateRequest req = new TranslateRequest("Hund", "de", List.of("it"));
    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andRespond(withServerError()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}
                """));

    assertThatThrownBy(() -> instance.translate(req))
        .isInstanceOf(TranslationFailedException.class)
        .hasMessageContaining("Claude translation call failed")
        .hasMessageContaining("500")
        .hasMessageContaining("Overloaded");
  }
}
