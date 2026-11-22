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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
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
  Translator translator;

  @Test
  void translate() {
    //given
    TranslateRequest request = new TranslateRequest("hello", "en", List.of("es"));
    when(translator.translate(request))
        .thenReturn(new TranslateResponse(List.of(new Translation("Hola", "es"))));

    //when
    TranslateResponse response = translateService.translate(request);

    //then
    assertThat(response.translations())
        .singleElement()
        .extracting(Translation::text, Translation::to)
        .containsExactly("Hola", "es");
    assertThat(translationUsageRepository.getCurrentMonthCount(YearMonth.now())).isEqualTo(1L);
  }

  @Test
  void translate_countsOneUnitPerTargetLanguage() {
    //given
    TranslateRequest request = new TranslateRequest("hello", "en", List.of("es", "it", "de"));
    when(translator.translate(any())).thenReturn(new TranslateResponse(List.of()));

    //when
    translateService.translate(request);

    //then
    assertThat(translationUsageRepository.getCurrentMonthCount(YearMonth.now())).isEqualTo(3L);
  }

  @Test
  void translate_quota_exceeded() {
    //given
    translationUsageRepository.set(YearMonth.now(), monthlyMaxTranslations);
    TranslateRequest request = new TranslateRequest("hello", "en", List.of("es"));

    //when-then
    assertThatExceptionOfType(QuotaExceededException.class)
        .isThrownBy(() -> translateService.translate(request))
        .withMessageContaining("Reached the translation usage");
    verify(translator, never()).translate(any());
  }

  @Test
  void translate_quota_exceeded_isNotCounted() {
    //given: 1 unit left, but the request needs 2
    translationUsageRepository.set(YearMonth.now(), monthlyMaxTranslations - 1);
    TranslateRequest request = new TranslateRequest("hello", "en", List.of("es", "it"));

    //when-then
    assertThatExceptionOfType(QuotaExceededException.class)
        .isThrownBy(() -> translateService.translate(request));
    verify(translator, never()).translate(any());
    assertThat(translationUsageRepository.getCurrentMonthCount(YearMonth.now()))
        .isEqualTo(monthlyMaxTranslations - 1);
  }

  @Test
  void translate_lastUnitOfQuota_isAllowed() {
    //given
    translationUsageRepository.set(YearMonth.now(), monthlyMaxTranslations - 1);
    TranslateRequest request = new TranslateRequest("hello", "en", List.of("es"));
    when(translator.translate(any())).thenReturn(new TranslateResponse(List.of()));

    //when
    translateService.translate(request);

    //then
    assertThat(translationUsageRepository.getCurrentMonthCount(YearMonth.now()))
        .isEqualTo(monthlyMaxTranslations);
    assertThat(translateService.isQuotaReached()).isTrue();
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
