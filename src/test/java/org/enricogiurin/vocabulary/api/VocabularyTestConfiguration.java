package org.enricogiurin.vocabulary.api;

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

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class VocabularyTestConfiguration {

  public static void main(String[] args) {
    SpringApplication.from(VocabularyApiApplication::main)
        .with(VocabularyTestConfiguration.class).run(args);
  }

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  KeycloakContainer keycloakContainer() {
    return new KeycloakContainer("quay.io/keycloak/keycloak:24.0.1")
            .withRealmImportFile("keycloak/vocabulary-realm.json")
            .withAdminUsername("admin")
            .withAdminPassword("Pa55w0rd");
  }

  @Bean
  @Primary
  Keycloak keycloak(KeycloakContainer container) {
    return KeycloakBuilder.builder()
            .serverUrl(container.getAuthServerUrl())
            .realm("master")
            .clientId("admin-cli")
            .grantType(OAuth2Constants.PASSWORD)
            .username("admin")
            .password("Pa55w0rd")
            .build();
  }

}
