package org.enricogiurin.vocabulary.api.conf;

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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

  @Bean
  public OpenAPI configOpenApi(
      @Value("${spring.application.name}") String name,
      @Value("${application.api.version}") String version,
      @Value("${application.api.description}") String description) {

    return new OpenAPI()
        .info(new Info()
            .title(name)
            .version(version)
            .description(description)
            .termsOfService("https://github.com/egch/vocabulary")
            .license(new License().name("Apache License, Version 2.0")
                .identifier("Apache-2.0")
                .url("https://opensource.org/license/apache-2-0/")))
        .components(new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes("googleOauth2", createOAuth2Scheme()))
        .addSecurityItem(new SecurityRequirement().addList("googleOauth2"));
  }

  private SecurityScheme createOAuth2Scheme() {
    return new SecurityScheme()
        .type(Type.OAUTH2)
        .flows(new OAuthFlows()
            .authorizationCode(new OAuthFlow()
                //http://localhost:9090/oauth2/authorization/google
                .authorizationUrl("http://localhost:9090/oauth2/authorization/google")

                .tokenUrl("https://oauth2.googleapis.com/token")
                .scopes(new Scopes().addString("email", "Access to Gmail"))));
  }
}
