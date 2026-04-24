package org.enricogiurin.vocabulary.api.rest.pub;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.enricogiurin.vocabulary.api.service.KeycloakClientService;
import org.enricogiurin.vocabulary.api.service.TurnstileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"hetzner", "test"})
@TestPropertySource(properties = "application.turnstile.secret-key=test-secret")
@Transactional
class RegisterUserControllerHetznerTest {

  @Autowired
  MockMvc mvc;

  @Value("${application.api.public-path}/users")
  String basePath;

  @MockitoBean
  KeycloakClientService keycloakClientService;

  @MockitoBean
  TurnstileService turnstileService;

  private static final String VALID_USER_JSON = """
      {
        "username": "mario-rossi",
        "firstName": "Mario",
        "lastName": "Rossi",
        "email": "new-user@vocabulary.org",
        "isAdmin": "false"
      }
      """;

  @Test
  void createNewUser_captchaVerificationPasses_returns201() throws Exception {
    when(turnstileService.verify(any())).thenReturn(true);

    mvc.perform(post(basePath)
            .header("CF-Turnstile-Response", "valid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_USER_JSON))
        .andExpect(status().isCreated());
  }

  @Test
  void createNewUser_captchaVerificationFails_returns403() throws Exception {
    when(turnstileService.verify(any())).thenReturn(false);

    mvc.perform(post(basePath)
            .header("CF-Turnstile-Response", "invalid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_USER_JSON))
        .andExpect(status().isForbidden());
  }
}
