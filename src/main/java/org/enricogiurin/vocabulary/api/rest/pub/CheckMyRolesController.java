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


import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${application.api.public-path}/roles")
@RequiredArgsConstructor
@Slf4j
class CheckMyRolesController {

  @GetMapping
  ResponseEntity<List<String>> roles(Authentication authentication) {
    if(authentication==null){
      log.warn("user not authenticated");
      return ResponseEntity.ok(List.of());
    }
    List<String> list = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
    list.forEach(s -> log.info("roles: {}", s));
    return ResponseEntity.ok(list);
  }

}
