package org.enricogiurin.vocabulary.api.service;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 - 2026 Vocabulary Team
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

import org.enricogiurin.vocabulary.api.anthropic.AnthropicProperties;
import org.enricogiurin.vocabulary.api.anthropic.ClaudeTranslator;
import org.enricogiurin.vocabulary.api.azure.AzureTranslator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks that {@code application.translation.provider} selects exactly one {@link Translator}.
 */
class TranslatorProviderSelectionTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(ClaudeTranslator.class, AzureTranslator.class)
      .withBean("anthropicRestClient", RestClient.class, RestClient::create)
      .withBean("translatorRestClient", RestClient.class, RestClient::create)
      .withBean(AnthropicProperties.class, () -> new AnthropicProperties("k", "m", "u"))
      .withBean(ObjectMapper.class, () -> JsonMapper.builder().build());

  @Test
  void defaultsToClaude() {
    runner.run(ctx -> {
      assertThat(ctx).hasSingleBean(Translator.class);
      assertThat(ctx.getBean(Translator.class)).isInstanceOf(ClaudeTranslator.class);
    });
  }

  @Test
  void claude() {
    runner.withPropertyValues("application.translation.provider=claude").run(ctx ->
        assertThat(ctx.getBean(Translator.class)).isInstanceOf(ClaudeTranslator.class));
  }

  @Test
  void azure() {
    runner.withPropertyValues("application.translation.provider=azure").run(ctx -> {
      assertThat(ctx).hasSingleBean(Translator.class);
      assertThat(ctx.getBean(Translator.class)).isInstanceOf(AzureTranslator.class);
    });
  }
}
