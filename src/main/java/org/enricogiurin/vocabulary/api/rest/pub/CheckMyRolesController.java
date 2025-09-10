package org.enricogiurin.vocabulary.api.rest.pub;


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
    list.forEach(s -> log.info("roles; {}", s));
    return ResponseEntity.ok(list);
  }

}
