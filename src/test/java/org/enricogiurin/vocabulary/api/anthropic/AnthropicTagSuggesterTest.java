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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.enricogiurin.vocabulary.api.model.Tag;
import org.enricogiurin.vocabulary.api.model.TagSuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AnthropicTagSuggesterTest {

  private MockRestServiceServer server;
  private AnthropicTagSuggester instance;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder()
        .baseUrl("https://mock.anthropic.local");

    server = MockRestServiceServer.bindTo(builder).build();

    AnthropicProperties properties = new AnthropicProperties(
        "test-api-key",
        "claude-haiku-4-5-20251001",
        "https://mock.anthropic.local"
    );
    instance = new AnthropicTagSuggester(builder.build(), properties);
  }

  @Test
  void suggestTags_returnsCorrectTagSuggestions() {
    // given
    String claudeResponse = """
        {
          "content": [
            {
              "type": "text",
              "text": "[{\\"tag\\":\\"TRAVEL\\",\\"label\\":\\"Viaggio\\"},{\\"tag\\":\\"TRANSPORT\\",\\"label\\":\\"Trasporto\\"},{\\"tag\\":\\"HOLIDAY\\",\\"label\\":\\"Vacanza\\"}]"
            }
          ]
        }
        """;

    server.expect(requestTo("https://mock.anthropic.local/v1/messages"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    List<Tag> tags = List.of(
        new Tag("IT", "Computers and technology"),
        new Tag("TRAVEL", "Trips and tourism"),
        new Tag("SPORT", "Sports and fitness"),
        new Tag("TRANSPORT", "Cars, trains, planes"),
        new Tag("HOLIDAY", "Vacations and leisure"),
        new Tag("FOOD", "Restaurants and meals")
    );

    // when
    List<TagSuggestion> result = instance.suggestTags(
        "domenica vado a venezia in aereo", "it", tags);

    // then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).tag()).isEqualTo("TRAVEL");
    assertThat(result.get(0).label()).isEqualTo("Viaggio");
    assertThat(result.get(1).tag()).isEqualTo("TRANSPORT");
    assertThat(result.get(1).label()).isEqualTo("Trasporto");
    assertThat(result.get(2).tag()).isEqualTo("HOLIDAY");
    assertThat(result.get(2).label()).isEqualTo("Vacanza");

    server.verify();
  }

  @Test
  void suggestTags_emptyResponseReturnsEmptyList() {
    // given
    String claudeResponse = """
        {
          "content": [
            {
              "type": "text",
              "text": "[]"
            }
          ]
        }
        """;

    server.expect(requestTo("https://mock.anthropic.local/v1/messages"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    // when
    List<TagSuggestion> result = instance.suggestTags("zzz", "en",
        List.of(new Tag("IT", "Computers"), new Tag("TRAVEL", "Trips")));

    // then
    assertThat(result).isEmpty();

    server.verify();
  }
}
