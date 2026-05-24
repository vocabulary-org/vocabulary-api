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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.learndeutsch.ExampleView;
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
@Import({VocabularyTestConfiguration.class, AnthropicExampleGeneratorTest.MockAnthropicClientConfig.class})
class AnthropicExampleGeneratorTest {

  @Autowired
  AnthropicExampleGenerator instance;

  @Autowired
  MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    server.reset();
  }

  @Test
  void generate_returnsExamplesFromClaudeResponse() {
    String claudeResponse = """
        {
          "content": [
            {
              "type": "text",
              "text": "[{\\"sentenceDe\\":\\"Der Hund schläft auf dem Sofa.\\",\\"sentenceTranslation\\":\\"Il cane dorme sul divano.\\"},{\\"sentenceDe\\":\\"Mein Hund heißt Jimmy.\\",\\"sentenceTranslation\\":\\"Il mio cane si chiama Jimmy.\\"}]"
            }
          ]
        }
        """;

    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    List<ExampleView> result = instance.generate("der", "Hund", "it", "cane");

    assertThat(result).hasSize(2);
    assertThat(result.get(0).sentenceDe()).isEqualTo("Der Hund schläft auf dem Sofa.");
    assertThat(result.get(0).sentenceTranslation()).isEqualTo("Il cane dorme sul divano.");
    assertThat(result.get(1).sentenceDe()).isEqualTo("Mein Hund heißt Jimmy.");
    assertThat(result.get(1).sentenceTranslation()).isEqualTo("Il mio cane si chiama Jimmy.");

    server.verify();
  }

  @Test
  void generate_emptyArrayResponseReturnsEmptyList() {
    String claudeResponse = """
        {
          "content": [{"type": "text", "text": "[]"}]
        }
        """;

    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    List<ExampleView> result = instance.generate("die", "Katze", "en", "cat");

    assertThat(result).isEmpty();
    server.verify();
  }

  @Test
  void parseExamples_plainJson() {
    String json = """
        [
          {"sentenceDe": "Das Kind spielt.", "sentenceTranslation": "The child plays."},
          {"sentenceDe": "Das Kind lacht.", "sentenceTranslation": "The child laughs."}
        ]
        """;

    List<ExampleView> result = instance.parseExamples(json);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).sentenceDe()).isEqualTo("Das Kind spielt.");
    assertThat(result.get(0).sentenceTranslation()).isEqualTo("The child plays.");
    assertThat(result.get(1).sentenceDe()).isEqualTo("Das Kind lacht.");
  }

  @Test
  void parseExamples_withCodeFences() {
    String json = """
        ```json
        [
          {"sentenceDe": "Das Haus ist groß.", "sentenceTranslation": "La casa è grande."}
        ]
        ```""";

    List<ExampleView> result = instance.parseExamples(json);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).sentenceDe()).isEqualTo("Das Haus ist groß.");
    assertThat(result.get(0).sentenceTranslation()).isEqualTo("La casa è grande.");
  }

  @Test
  void parseExamples_invalidJsonReturnsEmptyList() {
    List<ExampleView> result = instance.parseExamples("not valid json");

    assertThat(result).isEmpty();
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
