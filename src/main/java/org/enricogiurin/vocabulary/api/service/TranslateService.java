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

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.azure.AzureTranslator;
import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.enricogiurin.vocabulary.api.model.TranslateResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslateService {

  private final AzureTranslator azureTranslator;


    public TranslateResponse translate(TranslateRequest request) {
      List<AzureTranslator.AzureTranslateResponseItem> azure = azureTranslator.translate(request);

        List<TranslateResponse.Translation> translations = azure.stream()
                .flatMap(item -> item.translations().stream())
                .map(t -> new TranslateResponse.Translation(t.text(), t.to()))
                .toList();
        return new TranslateResponse(translations);
    }


}
