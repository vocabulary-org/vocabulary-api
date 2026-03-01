package org.enricogiurin.vocabulary.api.service;

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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Profile("hetzner")
@Slf4j
public class TurnstileService {

  static final String VERIFY_URL =
      "https://challenges.cloudflare.com/turnstile/v0/siteverify";

  private final String secretKey;
  private final RestClient restClient;

  public TurnstileService(
      @Value("${application.turnstile.secret-key}") String secretKey,
      RestClient turnstileRestClient) {
    this.secretKey = secretKey;
    this.restClient = turnstileRestClient;
  }

  public boolean verify(String token) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("secret", secretKey);
    formData.add("response", token);

    TurnstileResponse response = restClient.post()
        .uri(VERIFY_URL)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(formData)
        .retrieve()
        .body(TurnstileResponse.class);

    boolean success = response != null && response.success();
    log.debug("Turnstile verification result: {}", success);
    return success;
  }

  record TurnstileResponse(boolean success) {}

  @Configuration(proxyBeanMethods = false)
  @Profile("hetzner")
  static class TurnstileRestClientConfig {

    @Bean
    RestClient turnstileRestClient() {
      return RestClient.create();
    }
  }
}
