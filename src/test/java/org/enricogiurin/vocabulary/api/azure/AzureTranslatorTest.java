package org.enricogiurin.vocabulary.api.azure;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.enricogiurin.vocabulary.api.azure.AzureTranslator.API_VERSION;
import static org.enricogiurin.vocabulary.api.azure.AzureTranslator.API_VERSION_PARAM;
import static org.enricogiurin.vocabulary.api.azure.AzureTranslator.FROM_PARAM;
import static org.enricogiurin.vocabulary.api.azure.AzureTranslator.TO_PARAM;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.enricogiurin.vocabulary.api.exception.TranslationFailedException;
import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AzureTranslatorTest {

  private MockRestServiceServer server;
  private AzureTranslator instance;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder()
        // IMPORTANT: your production code doesn't add a path, only query params,
        // so baseUrl should already include the "/translate" path.
        .baseUrl("https://mock.azure.local/translate");

    server = MockRestServiceServer.bindTo(builder).build();

    RestClient restClient = builder.build();
    instance = new AzureTranslator(restClient);
  }

  @Test
  void translate_ok() {
    // given
    TranslateRequest req = new TranslateRequest(
        "Hello I am Enrico",
        "en",
        List.of("it", "es")
    );

    String azureJsonResponse = """
        [
          {
            "translations": [
              {"text": "Ciao sono Enrico", "to": "it"},
              {"text": "Hola soy Enrico", "to": "es"}
            ]
          }
        ]
        """;

    server.expect(requestTo(startsWith("https://mock.azure.local/translate")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(queryParam(API_VERSION_PARAM, API_VERSION))
        .andExpect(queryParam(FROM_PARAM, "en"))
        .andExpect(queryParam(TO_PARAM, "it", "es"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            [{"text":"Hello I am Enrico"}]
            """))
        .andRespond(withSuccess(azureJsonResponse, MediaType.APPLICATION_JSON));

    // when
    List<AzureTranslator.AzureTranslateResponseItem> result = instance.translate(req);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().translations()).hasSize(2);

    assertThat(result.getFirst().translations().getFirst().to()).isEqualTo("it");
    assertThat(result.getFirst().translations().getFirst().text()).isEqualTo("Ciao sono Enrico");

    assertThat(result.getFirst().translations().get(1).to()).isEqualTo("es");
    assertThat(result.getFirst().translations().get(1).text()).isEqualTo("Hola soy Enrico");

    server.verify();
  }

  @Test
  void translate_400() {
    // given
    TranslateRequest req = new TranslateRequest(
        "Hello",
        "en",
        List.of("it")
    );

    server.expect(requestTo(startsWith("https://mock.azure.local/translate")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withBadRequest()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {"error":{"code":400,"message":"Invalid request"}}
                """));

    // when / then
    assertThatThrownBy(() -> instance.translate(req))
        .isInstanceOf(TranslationFailedException.class)
        .hasMessageContaining("Azure Translator call failed:")
        .hasMessageContaining("400")
        .hasMessageContaining("Invalid request");

    server.verify();
  }


}
