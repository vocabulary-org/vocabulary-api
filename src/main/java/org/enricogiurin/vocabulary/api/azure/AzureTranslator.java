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

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.exception.TranslationFailedException;
import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.enricogiurin.vocabulary.api.model.TranslateResponse;
import org.enricogiurin.vocabulary.api.service.Translator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link Translator} backed by Azure AI Translator. Active only when
 * {@code application.translation.provider=azure}.
 */
@Component
@ConditionalOnProperty(name = "application.translation.provider", havingValue = "azure")
@RequiredArgsConstructor
@Slf4j
public class AzureTranslator implements Translator {

  static final String API_VERSION_PARAM = "api-version";
  static final String API_VERSION = "3.0";
  static final String FROM_PARAM = "from";
  static final String TO_PARAM = "to";

  private final RestClient translatorRestClient;

  @Override
  public TranslateResponse translate(TranslateRequest request) {
    List<AzureTranslateResponseItem> items = callAzure(request);
    List<TranslateResponse.Translation> translations = items == null ? List.of() : items.stream()
        .flatMap(item -> item.translations().stream())
        .map(t -> new TranslateResponse.Translation(t.text(), t.to()))
        .toList();
    return new TranslateResponse(translations);
  }

  private List<AzureTranslateResponseItem> callAzure(TranslateRequest request) {
    try {
      return translatorRestClient.post()
          .uri(builder -> {
            builder
                .queryParam(API_VERSION_PARAM, API_VERSION)
                .queryParam(FROM_PARAM, request.from());
            request.to().forEach(lang -> builder.queryParam(TO_PARAM, lang));
            return builder.build();
          })
          .contentType(MediaType.APPLICATION_JSON)
          .body(List.of(new AzureTranslateRequestItem(request.text())))
          .retrieve()
          .body(new ParameterizedTypeReference<>() {
          });
    } catch (RestClientResponseException ex) {
      throw new TranslationFailedException(
          "Azure Translator call failed: " + ex.getStatusCode() + " - "
              + ex.getResponseBodyAsString(),
          ex
      );
    }
  }

  public record AzureTranslateResponseItem(List<AzureTranslation> translations) {

    public record AzureTranslation(String text, String to) {

    }
  }

  private record AzureTranslateRequestItem(String text) {

  }
}
