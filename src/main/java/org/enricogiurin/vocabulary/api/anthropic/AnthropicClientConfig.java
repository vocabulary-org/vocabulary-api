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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
class AnthropicClientConfig {

  private static final String HEADER_API_KEY = "x-api-key";
  private static final String HEADER_ANTHROPIC_VERSION = "anthropic-version";
  private static final String ANTHROPIC_VERSION = "2023-06-01";

  @Bean
  RestClient anthropicRestClient(RestClient.Builder builder, AnthropicProperties props) {
    return builder
        .baseUrl(props.url())
        .defaultHeader(HEADER_API_KEY, props.apiKey())
        .defaultHeader(HEADER_ANTHROPIC_VERSION, ANTHROPIC_VERSION)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
