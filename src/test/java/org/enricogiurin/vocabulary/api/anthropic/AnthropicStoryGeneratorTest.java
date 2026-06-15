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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.exception.DataExecutionException;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.DeutschLevel;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GapCategory;
import org.enricogiurin.vocabulary.api.jooq.vocabulary.enums.GrammaticalCase;
import org.enricogiurin.vocabulary.api.learndeutsch.GeneratedStory;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryGapOptionView;
import org.enricogiurin.vocabulary.api.learndeutsch.StoryLength;
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
@Import({VocabularyTestConfiguration.class, AnthropicStoryGeneratorTest.MockAnthropicClientConfig.class})
class AnthropicStoryGeneratorTest {

  private static final String STORY_JSON = """
      {"title":"Marias Tag","body":"Maria geht in {{1}} Supermarkt.","gaps":[\
      {"position":1,"category":"ARTICLE","grammaticalCase":"AKKUSATIV","options":[\
      {"text":"der","correct":false},{"text":"den","correct":true},\
      {"text":"dem","correct":false},{"text":"des","correct":false}]}]}""";

  @Autowired
  AnthropicStoryGenerator instance;

  @Autowired
  MockRestServiceServer server;

  @Autowired
  ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    server.reset();
  }

  @Test
  void generate_returnsStoryFromClaudeResponse() {
    String claudeResponse = """
        {
          "content": [
            {
              "type": "tool_use",
              "id": "toolu_1",
              "name": "save_story",
              "input": %s
            }
          ]
        }
        """.formatted(STORY_JSON);

    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    GeneratedStory result = instance.generate(DeutschLevel.A2, "food", StoryLength.SHORT);

    assertThat(result.title()).isEqualTo("Marias Tag");
    assertThat(result.body()).contains("{{1}}");
    assertThat(result.gaps()).hasSize(1);
    assertThat(result.gaps().getFirst().position()).isEqualTo(1);
    assertThat(result.gaps().getFirst().category()).isEqualTo(GapCategory.ARTICLE);
    assertThat(result.gaps().getFirst().grammaticalCase()).isEqualTo(GrammaticalCase.AKKUSATIV);
    assertThat(result.gaps().getFirst().options())
        .filteredOn(StoryGapOptionView::correct)
        .singleElement()
        .extracting(StoryGapOptionView::text)
        .isEqualTo("den");

    server.verify();
  }

  @Test
  void generateFromText_returnsStoryWithDetectedLevel() {
    String storyWithLevel = "{\"level\":\"B1\"," + STORY_JSON.substring(1);
    String claudeResponse = """
        {
          "content": [
            {
              "type": "tool_use",
              "id": "toolu_1",
              "name": "save_story",
              "input": %s
            }
          ]
        }
        """.formatted(storyWithLevel);

    server.expect(requestTo(Matchers.endsWith("/v1/messages")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(claudeResponse, MediaType.APPLICATION_JSON));

    GeneratedStory result = instance.generateFromText("Maria geht in den Supermarkt.", null);

    assertThat(result.title()).isEqualTo("Marias Tag");
    assertThat(result.level()).isEqualTo(DeutschLevel.B1);
    assertThat(result.gaps()).hasSize(1);

    server.verify();
  }

  @Test
  void parseStory_fromToolInput() throws Exception {
    GeneratedStory result = instance.parseStory(objectMapper.readTree(STORY_JSON));

    assertThat(result.title()).isEqualTo("Marias Tag");
    assertThat(result.gaps()).hasSize(1);
  }

  @Test
  void parseStory_invalidInputThrows() throws Exception {
    JsonNode notAnObject = objectMapper.readTree("[]");

    assertThatThrownBy(() -> instance.parseStory(notAnObject))
        .isInstanceOf(DataExecutionException.class);
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
