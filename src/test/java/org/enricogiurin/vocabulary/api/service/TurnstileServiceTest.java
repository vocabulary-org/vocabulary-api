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

import static org.assertj.core.api.Assertions.assertThat;
import static org.enricogiurin.vocabulary.api.service.TurnstileService.VERIFY_URL;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import org.enricogiurin.vocabulary.api.service.TurnstileService.TurnstileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class TurnstileServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  RestClient restClient;

  TurnstileService turnstileService;

  @BeforeEach
  void setUp() {
    turnstileService = new TurnstileService("test-secret", restClient);
  }

  @Test
  void verify_returnsTrue_whenCloudflareRespondsSuccess() {
    when(restClient.post()
        .uri(eq(VERIFY_URL))
        .contentType(eq(MediaType.APPLICATION_FORM_URLENCODED))
        .body(any(MultiValueMap.class))
        .retrieve()
        .body(eq(TurnstileResponse.class)))
        .thenReturn(new TurnstileResponse(true));

    assertThat(turnstileService.verify("valid-token")).isTrue();
  }

  @Test
  void verify_returnsFalse_whenCloudflareRespondsFailure() {
    assertThat(turnstileService.verify("invalid-token")).isFalse();
  }

  @Test
  void verify_returnsFalse_whenCloudflareReturnsNull() {
    when(restClient.post()
        .uri(eq(VERIFY_URL))
        .contentType(eq(MediaType.APPLICATION_FORM_URLENCODED))
        .body(any(MultiValueMap.class))
        .retrieve()
        .body(eq(TurnstileResponse.class)))
        .thenReturn(null);

    assertThat(turnstileService.verify("any-token")).isFalse();
  }
}
