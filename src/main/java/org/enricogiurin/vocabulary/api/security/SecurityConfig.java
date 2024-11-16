package org.enricogiurin.vocabulary.api.security;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 Vocabulary Team
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      final @Value("${application.api.base-path}") String basePath) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())  // Disables CSRF protection
        .authorizeHttpRequests(registry -> {
          registry.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
          registry.requestMatchers(basePath + "/user/**").hasRole(Roles.USER.name());
          registry.requestMatchers(basePath + "/admin/**").hasRole(Roles.ADMIN.name());
          registry.requestMatchers(basePath + "/authenticated/**").authenticated();
          registry.requestMatchers(basePath + "/public/**").permitAll();
          registry.anyRequest().authenticated();
        })
        .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl(
            "/")) // Redirect to the original URL after authentication
        //.oauth2Login(Customizer.withDefaults())
        .formLogin(Customizer.withDefaults())
        .build();
  }
}
