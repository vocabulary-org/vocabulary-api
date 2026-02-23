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


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.model.UserLanguages;
import org.enricogiurin.vocabulary.api.security.CurrentUser;
import org.enricogiurin.vocabulary.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing Language resources.
 */
@RestController
@RequestMapping("${application.api.user-path}/user-languages")
@RequiredArgsConstructor
@Slf4j
public class UserLanguagesController {

  private final CurrentUser currentUser;

  private final UserService userService;


  @GetMapping
  public ResponseEntity<UserLanguages> getUserLanguages() {
    log.info("GET /user-languages - subject: {}", currentUser.getSubject());
    return userService.userLanguages(currentUser.getUserId())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.ok(UserLanguages.empty()));
  }

  @PutMapping
  public ResponseEntity<UserLanguages> storeUserLanguages(@RequestBody UserLanguages userLanguages) {
    log.info("PUT /user-languages - subject: {}", currentUser.getSubject());
    UserLanguages result = userService.saveUserLanguages(userLanguages, currentUser.getUserId());
    return ResponseEntity.ok(result);
  }

}
