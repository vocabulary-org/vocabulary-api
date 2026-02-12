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
import org.enricogiurin.vocabulary.api.security.CurrentUser;
import org.enricogiurin.vocabulary.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("${application.api.user-path}/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;
  private final CurrentUser currentUser;


  @DeleteMapping()
  public ResponseEntity<Message> deleteUser() {

    log.info("Request to delete user with subject: {}", currentUser.getSubject());
    userService.deleteUserByKeycloakId(currentUser.getSubject());
    return ResponseEntity.noContent().build(); // 204
  }

}
