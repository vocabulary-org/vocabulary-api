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

import java.time.YearMonth;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.azure.AzureTranslator;
import org.enricogiurin.vocabulary.api.exception.QuotaExceededException;
import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.enricogiurin.vocabulary.api.model.TranslateResponse;
import org.enricogiurin.vocabulary.api.repository.TranslationUsageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TranslateService {

  private final AzureTranslator azureTranslator;
  private final TranslationUsageRepository translationUsageRepository;
  private final long monthlyMaxTranslations;

  public TranslateService(
      AzureTranslator azureTranslator,
      TranslationUsageRepository translationUsageRepository,
      @Value("${application.translation.monthly-max-translations}") long monthlyMaxTranslations
  ) {
    this.azureTranslator = azureTranslator;
    this.translationUsageRepository = translationUsageRepository;
    this.monthlyMaxTranslations = monthlyMaxTranslations;
  }

  public TranslateResponse translate(TranslateRequest request) {
    YearMonth now = YearMonth.now();
    long cnt = translationUsageRepository.incrementMonthlyCounter(now);
    log.info("translation usage for {} is equal to {}", now, cnt);
    if (cnt > monthlyMaxTranslations) {
      throw new QuotaExceededException(
          "Reached the translation usage limit for the current month!");
    }
    List<AzureTranslator.AzureTranslateResponseItem> azure = azureTranslator.translate(request);

    List<TranslateResponse.Translation> translations = azure.stream()
        .flatMap(item -> item.translations().stream())
        .map(t -> new TranslateResponse.Translation(t.text(), t.to()))
        .toList();
    return new TranslateResponse(translations);
  }

  /**
   * Checks whether the configured monthly quota has been reached.
   *
   * @return true if the quota is reached or exceeded, false otherwise
   */
  public boolean isQuotaReached() {
    YearMonth now = YearMonth.now();
    return translationUsageRepository.getCurrentMonthCount(now) >= monthlyMaxTranslations;
  }


}
