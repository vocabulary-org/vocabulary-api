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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.azure.AzureTranslator;
import org.enricogiurin.vocabulary.api.exception.QuotaExceededException;
import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.enricogiurin.vocabulary.api.model.TranslateResponse;
import org.enricogiurin.vocabulary.api.model.TranslateResponse.Translation;
import org.enricogiurin.vocabulary.api.repository.TranslationUsageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Import({VocabularyTestConfiguration.class})
@Transactional
class TranslateServiceTest {

  @Autowired
  TranslateService translateService;

  @Autowired
  TranslationUsageRepository translationUsageRepository;

  @Value("${application.translation.monthly-max-translations}")
  long monthlyMaxTranslations;

  @MockitoBean
  AzureTranslator azureTranslator;

  @Test
  void translate() {
    //given
    List<AzureTranslator.AzureTranslateResponseItem> mockResponse = List.of(
        new AzureTranslator.AzureTranslateResponseItem(
            List.of(new AzureTranslator.AzureTranslateResponseItem.AzureTranslation("Hola", "es"))
        )
    );
    TranslateRequest request = new TranslateRequest("hello", "en", List.of("es"));
    when(azureTranslator.translate(request)).thenReturn(mockResponse);
    //when
    TranslateResponse response = translateService.translate(request);
    //then
    assertNotNull(response);
    assertThat(response.translations())
        .singleElement()
        .extracting(
            Translation::text,
            Translation::to
        )
        .containsExactly("Hola", "es");
  }

  @Test
  void translate_quota_exceeded() {
    //given
    translationUsageRepository.set(YearMonth.now(), monthlyMaxTranslations);
    TranslateRequest request = new TranslateRequest("hello", "en", List.of("es"));
    //when-then
    assertThatExceptionOfType(QuotaExceededException.class)
        .isThrownBy(() -> {
          translateService.translate(request);
        }).withMessageContaining("Reached the translation usage");
    verify(azureTranslator, never()).translate(request);

  }

  @Test
  void isQuotaReached() {
    //given
    translationUsageRepository.set(YearMonth.now(), monthlyMaxTranslations);
    //when
    boolean quotaReached = translateService.isQuotaReached();
    //then
    assertThat(quotaReached).isTrue();
  }

  @Test
  void isQuotaReached_false() {
    //given
    translationUsageRepository.set(YearMonth.now(), monthlyMaxTranslations - 1);
    //when
    boolean quotaReached = translateService.isQuotaReached();
    //then
    assertThat(quotaReached).isFalse();
  }
}
