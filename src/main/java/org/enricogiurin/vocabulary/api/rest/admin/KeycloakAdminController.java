package org.enricogiurin.vocabulary.api.rest.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${application.api.admin-path}/keycloak")
@RequiredArgsConstructor
public class KeycloakAdminController {

}
