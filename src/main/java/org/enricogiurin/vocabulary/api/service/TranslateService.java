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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.conf.AzureTranslatorProperties;
import org.enricogiurin.vocabulary.api.exception.TranslationFailedException;
import org.enricogiurin.vocabulary.api.model.AzureTranslateRequestItem;
import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.enricogiurin.vocabulary.api.model.TranslateResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslateService {
    private final RestClient translatorRestClient;
    private final AzureTranslatorProperties props;


    public TranslateResponse translate(TranslateRequest request) {
        List<AzureTranslateResponseItem> azure = callAzure(request);

        List<TranslateResponse.Translation> translations = azure.stream()
                .flatMap(item -> item.translations().stream())
                .map(t -> new TranslateResponse.Translation(t.text(), t.to()))
                .toList();
        return new TranslateResponse(translations);
    }


    private List<AzureTranslateResponseItem> callAzure(TranslateRequest request) {
        try {
            return translatorRestClient.post()
                    .uri(b -> {
                        b
                                .queryParam("api-version", "3.0")
                                .queryParam("from", request.from());
                        request.to().forEach(lang -> b.queryParam("to", lang));
                        return b.build();
                    })
                    .header("Ocp-Apim-Subscription-Key", props.key())
                    .header("Ocp-Apim-Subscription-Region", props.region())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(new AzureTranslateRequestItem(request.text())))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientResponseException ex) {
            // Optional: wrap into your own exception type
            throw new TranslationFailedException(
                    "Azure Translator call failed: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    private record AzureTranslateResponseItem(List<AzureTranslation> translations) {
        private record AzureTranslation(String text, String to) {
        }
    }
}
