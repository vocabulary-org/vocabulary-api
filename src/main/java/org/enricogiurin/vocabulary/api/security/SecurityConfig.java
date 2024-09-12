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


import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enricogiurin.vocabulary.api.model.User;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

  private final UserRepository userRepository;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      final @Value("${application.api.base-path}") String basePath) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())  // Disables CSRF protection
        .authorizeHttpRequests(registry -> {
          registry.requestMatchers("/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
          registry.requestMatchers(basePath + "/user/**").hasRole(Roles.USER.name());
          registry.requestMatchers(basePath + "/admin/**").hasRole(Roles.ADMIN.name());
          registry.requestMatchers(basePath + "/authenticated/**").authenticated();
          registry.requestMatchers(basePath + "/public/**").permitAll();
          registry.anyRequest().authenticated();
        })
        .oauth2Login(Customizer.withDefaults())
        .formLogin(Customizer.withDefaults())
        .build();
  }

  //TODO - refactor this
  @Bean
  GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return (authorities) -> {
      Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
      authorities.forEach(authority -> {
        if (OidcUserAuthority.class.isInstance(authority)) {
          log.warn("OidcUserAuthority case not implemented");
        } else if (OAuth2UserAuthority.class.isInstance(authority)) {
          OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;
          Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();
          if (userAttributes.get("email") instanceof String authenticatedEmail) {
            log.info("Authenticated user's email: {}", authenticatedEmail);
            // Map the attributes found in userAttributes
            // to one or more GrantedAuthority's and add it to mappedAuthorities
            Optional<User> optionalUser = userRepository.findByEmail(authenticatedEmail);
            if (optionalUser.isPresent()) {
              mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + Roles.USER.name()));
              if (optionalUser.get().isAdmin()) {
                mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + Roles.ADMIN.name()));
              }

            } else {
              log.info("{} is an authenticated user but not registered yet!", authenticatedEmail);
            }

          }
        }
      });
      return mappedAuthorities;
    };
  }
}
