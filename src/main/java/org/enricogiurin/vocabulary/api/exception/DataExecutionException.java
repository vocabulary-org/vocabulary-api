package org.enricogiurin.vocabulary.api.exception;

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

/**
 * Indicates an unexpected error occurred during data execution. This exception is typically thrown
 * when there is an unexpected issue during the execution of a database query or operation.
 */
public class DataExecutionException extends RuntimeException {

  public DataExecutionException(String message) {
    super(message);
  }

  public DataExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
