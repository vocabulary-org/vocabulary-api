package org.enricogiurin.vocabulary.api.exception;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 - 2026 Vocabulary Team
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

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class APIExceptionHandlerTest {

  APIExceptionHandler handler;
  ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    handler = new APIExceptionHandler();
    listAppender = new ListAppender<>();
    listAppender.start();
    ((Logger) LoggerFactory.getLogger(APIExceptionHandler.class)).addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    ((Logger) LoggerFactory.getLogger(APIExceptionHandler.class)).detachAppender(listAppender);
  }

  @Test
  void logException_logsOriginWhenStackTraceIsPresent() {
    DataNotFoundException e = new DataNotFoundException("resource not found");
    StackTraceElement origin = e.getStackTrace()[0];

    handler.logException(e);

    assertThat(listAppender.list).hasSize(1);
    assertThat(origin.getClassName()).isEqualTo(APIExceptionHandlerTest.class.getName());
    assertThat(listAppender.list.get(0).getFormattedMessage())
        .contains("DataNotFoundException")
        .contains(origin.getClassName())
        .contains(origin.getMethodName())
        .contains(String.valueOf(origin.getLineNumber()))
        .contains("resource not found");
  }

  @Test
  void logException_doesNotThrowWhenStackTraceIsEmpty() {
    DataNotFoundException e = new DataNotFoundException("resource not found");
    e.setStackTrace(new StackTraceElement[0]);

    handler.logException(e);

    assertThat(listAppender.list).hasSize(1);
    assertThat(listAppender.list.get(0).getFormattedMessage())
        .contains("DataNotFoundException")
        .contains("resource not found")
        .doesNotContain(APIExceptionHandlerTest.class.getName());
  }
}
