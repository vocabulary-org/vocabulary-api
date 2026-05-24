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

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
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
@Import({VocabularyTestConfiguration.class, AnthropicTranslationGeneratorTest.MockAnthropicClientConfig.class})
class AnthropicTranslationGeneratorTest {

  @Autowired
  AnthropicTranslationGenerator instance;

  @Autowired
  MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    server.reset();
  }

  @Test
  void generate_returnsTranslationFromClaudeResponse() {
    String claudeResponse = """
        {
          "content": [{"type": "text", "text": "dog"}]
        }
        """;

    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    String result = instance.generate("der", "Hund", "en");

    assertThat(result).isEqualTo("dog");
    server.verify();
  }

  @Test
  void generate_stripsWhitespaceFromResponse() {
    String claudeResponse = """
        {
          "content": [{"type": "text", "text": "  pear  "}]
        }
        """;

    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    String result = instance.generate("die", "Birne", "en");

    assertThat(result).isEqualTo("pear");
    server.verify();
  }

  @Test
  void generate_spanishTranslation() {
    String claudeResponse = """
        {
          "content": [{"type": "text", "text": "perro"}]
        }
        """;

    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    String result = instance.generate("der", "Hund", "es");

    assertThat(result).isEqualTo("perro");
    server.verify();
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
