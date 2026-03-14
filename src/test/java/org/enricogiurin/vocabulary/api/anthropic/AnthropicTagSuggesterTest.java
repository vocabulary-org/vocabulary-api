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
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.model.TagSuggestion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import({VocabularyTestConfiguration.class, AnthropicTagSuggesterTest.MockAnthropicClientConfig.class})
class AnthropicTagSuggesterTest {


  @Autowired
  AnthropicTagSuggester instance;

  @Autowired
  MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    server.reset();
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

    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    // when
    List<TagSuggestion> result = instance.suggestTags("domenica vado a venezia in aereo", "it");

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

    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    // when
    List<TagSuggestion> result = instance.suggestTags("zzz", "en");

    // then
    assertThat(result).isEmpty();

    server.verify();
  }


  @Test
  void init_systemPromptContainsAllTags() {
    assertThat(instance.getSystemPrompt()).isNotBlank();
    assertThat(instance.getSystemPrompt()).contains("- EDUCATION: Schools, studying, teachers, exams, universities, and learning");
    assertThat(instance.getSystemPrompt()).contains("- TRAVEL: Trips, flights, hotels, destinations, tourism, and exploration");
    assertThat(instance.getSystemPrompt()).contains("- FINANCE: Money, banks, investments, taxes, savings, and financial matters");
  }

  @Test
  void parseTagSuggestions_plainJson() {
    String json = """
        [
          {"tag": "TRAVEL", "label": "Travel"},
          {"tag": "NATURE", "label": "Nature"}
        ]
        """;

    List<TagSuggestion> result = instance.parseTagSuggestions(json);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).tag()).isEqualTo("TRAVEL");
    assertThat(result.get(0).label()).isEqualTo("Travel");
    assertThat(result.get(1).tag()).isEqualTo("NATURE");
    assertThat(result.get(1).label()).isEqualTo("Nature");
  }

  @Test
  void parseTagSuggestions_withCodeFences() {
    String json = """
        ```json
        [
          {"tag": "TRAVEL", "label": "Travel"},
          {"tag": "NATURE", "label": "Nature"}
        ]
        ```""";

    List<TagSuggestion> result = instance.parseTagSuggestions(json);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).tag()).isEqualTo("TRAVEL");
    assertThat(result.get(1).tag()).isEqualTo("NATURE");
  }

  @TestConfiguration
  static class MockAnthropicClientConfig {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();

    @Bean
    @Primary
    RestClient anthropicRestClient() {
      return builder.build();
    }

    @Bean
    MockRestServiceServer mockRestServiceServer() {
      return mockServer;
    }
  }

}
