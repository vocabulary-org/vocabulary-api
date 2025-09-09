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


import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.rest.admin.KeycloakUserResponse;
import org.enricogiurin.vocabulary.api.service.KeycloakClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${application.api.public-path}/users")
@RequiredArgsConstructor
public class RegisterUserController {

  private final KeycloakClientService keycloakAdminService;

  @PostMapping()
  public ResponseEntity<KeycloakUserResponse> createNewKeycloakUser(
      @RequestBody KeycloakUser keycloakUser) {
    keycloakAdminService.createNewUser(keycloakUser);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(KeycloakUserResponse.builder()
            .username(keycloakUser.username())
            .build());
  }

}
