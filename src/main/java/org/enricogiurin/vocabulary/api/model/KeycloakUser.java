package org.enricogiurin.vocabulary.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.enricogiurin.vocabulary.api.validation.ValidationGroups;

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
@Builder
public record KeycloakUser(

    @NotNull(message = USERNAME_NOT_NULL_CONSTRAINT, groups = ValidationGroups.Post.class)
    @Size(min = 3, max = 20, message = USERNAME_CONSTRAINT, groups = {
        ValidationGroups.Post.class, ValidationGroups.Patch.class})
    String username,

    @NotNull(message = FIRST_NAME_NOT_NULL_CONSTRAINT, groups = ValidationGroups.Post.class)
    @Size(min = 3, max = 20, message = FIRST_NAME_CONSTRAINT, groups = {
        ValidationGroups.Post.class, ValidationGroups.Patch.class})
    String firstName,

    @NotNull(message = LAST_NAME_NOT_NULL_CONSTRAINT, groups = ValidationGroups.Post.class)
    @Size(min = 3, max = 20, message = LAST_NAME_CONSTRAINT, groups = {
        ValidationGroups.Post.class, ValidationGroups.Patch.class})
    String lastName,

    @NotNull(message = EMAIL_NOT_NULL_CONSTRAINT, groups = ValidationGroups.Post.class)
    @Email(message = EMAIL_CONSTRAINT, groups = {ValidationGroups.Post.class,
        ValidationGroups.Patch.class})
    String email,
    boolean isAdmin) {

  public static final String USERNAME_NOT_NULL_CONSTRAINT = "username must not be null";
  public static final String USERNAME_CONSTRAINT = "username must be between 3 and 20 characters";

  public static final String FIRST_NAME_NOT_NULL_CONSTRAINT = "first name must not be null";
  public static final String FIRST_NAME_CONSTRAINT = "first name must be between 3 and 20 characters";

  public static final String LAST_NAME_NOT_NULL_CONSTRAINT = "last name must not be null";
  public static final String LAST_NAME_CONSTRAINT = "last name must be between 3 and 20 characters";

  public static final String EMAIL_NOT_NULL_CONSTRAINT = "email must not be null";
  public static final String EMAIL_CONSTRAINT = "email must be a valid email address";


}


