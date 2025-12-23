package org.enricogiurin.vocabulary.api.rest.me;

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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.azure.AzureTranslator;
import org.enricogiurin.vocabulary.api.azure.AzureTranslator.AzureTranslateResponseItem;
import org.enricogiurin.vocabulary.api.azure.AzureTranslator.AzureTranslateResponseItem.AzureTranslation;
import org.enricogiurin.vocabulary.api.model.TranslateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class LanguageTranslateControllerTest {

  @Autowired
  MockMvc mvc;

  @Value("${application.api.user-path}/translate")
  String basePath;

  @MockitoBean
  AzureTranslator azureTranslator;


  @Test
  void translate() throws Exception {
    when(azureTranslator.translate(any()))
        .thenReturn(List.of(
            new AzureTranslateResponseItem(List.of(
                new AzureTranslation("ciao", "it"),
                new AzureTranslation("hola", "es"))
            )));
    final String body =
        """
            {
              "text": "Hello",
              "from": "en",
              "to": [
                "it", "es"
              ]
            }""";
    mvc.perform(post(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.translations", hasSize(2)))
        .andExpect(jsonPath("$.translations[0].text", is("ciao")))
        .andExpect(jsonPath("$.translations[0].to", is("it")))
        .andExpect(jsonPath("$.translations[1].text", is("hola")))
        .andExpect(jsonPath("$.translations[1].to", is("es")))
    ;
  }

  @Test
  void translate_badRequest() throws Exception {
    final String notValidSentence = """
        This is a deliberately long string created to be longer than one hundred characters so it can
        be used for testing validation, limits, or UI behavior."""
        .replace("\n", "\\n");
    assertThat(notValidSentence).hasSizeGreaterThan(100);

    final String body =
        """
            {
              "text": "%s",
              "from": "en",
              "to": [
                "it", "es"
              ]
            }""".formatted(notValidSentence);
    mvc.perform(post(basePath)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString(TranslateRequest.TEXT_CONSTRAINT)))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void isTranslationQuotaReached() throws Exception {
    mvc.perform(get(basePath + "/quota-reached"))
        .andExpect(status().isOk())
        .andExpect(content().string("false"));

  }
}
