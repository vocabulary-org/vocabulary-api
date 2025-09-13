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

import com.yourrents.services.common.util.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
class APIExceptionHandler extends ResponseEntityExceptionHandler {
  // ---------- Domain exceptions ----------

  @ExceptionHandler(DataNotFoundException.class)
  public ResponseEntity<Object> dataNotFound(DataNotFoundException e, NativeWebRequest request) {
    logger.error(e.getMessage(), e);
    return super.handleExceptionInternal(e,
        buildErrorResponse(e.getMessage(), e, request, HttpStatus.NOT_FOUND.value()),
        new HttpHeaders(), HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(DataConflictException.class)
  public ResponseEntity<Object> dataConflict(DataConflictException e, NativeWebRequest request) {
    logger.error(e.getMessage(), e);
    return super.handleExceptionInternal(e,
        buildErrorResponse(e.getMessage(), e, request, HttpStatus.CONFLICT.value()),
        new HttpHeaders(), HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(DataExecutionException.class)
  public ResponseEntity<Object> dataExecution(DataExecutionException e, NativeWebRequest request) {
    logger.error(e.getMessage(), e);
    return super.handleExceptionInternal(e,
        buildErrorResponse(e.getMessage(), e, request, HttpStatus.INTERNAL_SERVER_ERROR.value()),
        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  @ExceptionHandler(KeycloakException.class)
  public ResponseEntity<Object> keycloak(KeycloakException e, NativeWebRequest request) {
    logger.error(e.getMessage(), e);
    return super.handleExceptionInternal(e,
        buildErrorResponse(e.getMessage(), e, request, HttpStatus.INTERNAL_SERVER_ERROR.value()),
        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  /*
   **** helpers ****
   */
  private ApiError buildErrorResponse(String message, Exception e, NativeWebRequest request,
      int status) {
    return new ApiError(message, e.getMessage(), status,
        ((HttpServletRequest) (request.getNativeRequest())).getRequestURI());
  }

}
