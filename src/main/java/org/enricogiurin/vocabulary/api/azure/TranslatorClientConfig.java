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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
class TranslatorClientConfig {
  private static final String HEADER_SUBSCRIPTION_KEY =
      "Ocp-Apim-Subscription-Key";

  private static final String HEADER_SUBSCRIPTION_REGION =
      "Ocp-Apim-Subscription-Region";


  @Bean
  RestClient translatorRestClient(RestClient.Builder builder,
      final AzureTranslatorProperties props,

      @Value("${translator.azure.url}") String url) {
    return builder
        .baseUrl(url)
        .defaultHeader(HEADER_SUBSCRIPTION_KEY, props.key())
        .defaultHeader(HEADER_SUBSCRIPTION_REGION, props.region())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
